package org.mcphackers.launchwrapper.micromixin.transformer;

import java.util.Arrays;
import java.util.List;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.RecordComponentNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.stianloader.remapper.MemberRef;

/**
 * Simple in-memory remapping engine. Unlike many other remappers it is able to take in already parsed
 * {@link org.objectweb.asm.tree.ClassNode Objectweb ASM Classnodes} as input and output them without having
 * to go through an intermediary store-to-file mode.
 * Additionally, this is a no-bullshit remapper. It will only remap, not change your Access flags,
 * LVT entries or anything like that. If you want a deobfuscator,
 * use {@link de.geolykt.starloader.obftools.asm.Oaktree} after remapping.
 */
public final class Remapper {
	// private final MethodRenameMap hierarchisedMethodRenames = new MethodRenameMap();
	// private final FieldRenameMap hierarchisedFieldRenames = new FieldRenameMap();
	
	private Mappings mappings;
	
	public Remapper(Mappings mappingData) {
		mappings = mappingData;
	}

	/**
	 * Processes all remap orders and clears the remap orders afterwards. The classes that need to be processed remain in the targets
	 * list until {@link #clearTargets()} is invoked. This allows for reusability of the same remapper instance.
	 * Class names are remapped last.
	 */
	public void remap(ClassNode node) {
		StringBuilder sharedStringBuilder = new StringBuilder();

        for (FieldNode field : node.fields) {
            remapField(node.name, field, sharedStringBuilder);
        }
        for (InnerClassNode innerClass : node.innerClasses) {
            String newInnerName = mappings.classes.get(innerClass.innerName);
            if (newInnerName != null) {
                innerClass.innerName = newInnerName;
            }
            String newOuterName = mappings.classes.get(innerClass.outerName);
            if (newOuterName != null) {
                innerClass.outerName = newOuterName;
            }
            String newName = mappings.classes.get(innerClass.name);
            if (newName != null) {
                innerClass.name = newName;
            }
        }
        for (int i = 0; i < node.interfaces.size(); i++) {
            String newInterfaceName = mappings.classes.get(node.interfaces.get(i));
            if (newInterfaceName != null) {
                node.interfaces.set(i, newInterfaceName);
            }
        }
        remapAnnotations(node.invisibleTypeAnnotations, sharedStringBuilder);
        remapAnnotations(node.invisibleAnnotations, sharedStringBuilder);
        remapAnnotations(node.visibleTypeAnnotations, sharedStringBuilder);
        remapAnnotations(node.visibleAnnotations, sharedStringBuilder);
        for (MethodNode method : node.methods) {
            remapMethod(node, method, sharedStringBuilder);
        }
        if (node.nestHostClass != null) {
            node.nestHostClass = remapInternalName(node.nestHostClass, sharedStringBuilder);
        }
        if (node.nestMembers != null) {
            int size = node.nestMembers.size();
            for (int i = 0; i < size; i++) {
                String member = node.nestMembers.get(i);
                String remapped = remapInternalName(member, sharedStringBuilder);
                if (member != remapped) {
                    node.nestMembers.set(i, remapped);
                }
            }
        }
        if (node.outerClass != null) {
            if (node.outerMethod != null && node.outerMethodDesc != null) {
                node.outerMethod = mappings.methods.getOrDefault(new MemberRef(node.outerClass, node.outerMethod, node.outerMethodDesc), node.outerMethod);
            }
            node.outerClass = remapInternalName(node.outerClass, sharedStringBuilder);
        }
        if (node.outerMethodDesc != null) {
            sharedStringBuilder.setLength(0);
            if (remapSignature(node.outerMethodDesc, sharedStringBuilder)) {
                node.outerMethodDesc = sharedStringBuilder.toString();
            }
        }
        if (node.permittedSubclasses != null) {
            int size = node.permittedSubclasses.size();
            for (int i = 0; i < size; i++) {
                String member = node.permittedSubclasses.get(i);
                String remapped = remapInternalName(member, sharedStringBuilder);
                if (member != remapped) {
                    node.permittedSubclasses.set(i, remapped);
                }
            }
        }
        if (node.recordComponents != null) {
            // This requires eventual testing as I do not make use of codesets with Java9+ features.
            for (RecordComponentNode record : node.recordComponents) {
                sharedStringBuilder.setLength(0);
                if (remapSignature(record.descriptor, sharedStringBuilder)) {
                    record.descriptor = sharedStringBuilder.toString();
                }
                remapAnnotations(record.invisibleAnnotations, sharedStringBuilder);
                remapAnnotations(record.invisibleTypeAnnotations, sharedStringBuilder);
                remapAnnotations(record.visibleAnnotations, sharedStringBuilder);
                remapAnnotations(record.visibleTypeAnnotations, sharedStringBuilder);
                if (record.signature != null) {
                    sharedStringBuilder.setLength(0);
                    if (remapSignature(record.signature, sharedStringBuilder)) {
                        record.signature = sharedStringBuilder.toString();
                    }
                }
            }
        }
        if (node.signature != null) {
            sharedStringBuilder.setLength(0);
            // Class signatures are formatted differently than method or field signatures, but we can just ignore this
            // caveat here as the method will consider the invalid tokens are primitive objects. (sometimes laziness pays off)
            if (remapSignature(node.signature, sharedStringBuilder)) {
                node.signature = sharedStringBuilder.toString();
            }
        }
        if (node.superName != null) {
            node.superName = remapInternalName(node.superName, sharedStringBuilder);
        }
        // remap the node's name if required
        String newName = mappings.classes.get(node.name);
        if (newName != null) {
            node.name = newName;
        }
	}

	private void remapAnnotation(AnnotationNode annotation, StringBuilder sharedStringBuilder) {
		String internalName = annotation.desc.substring(1, annotation.desc.length() - 1);
		String newInternalName = mappings.classes.get(internalName);
		if (newInternalName != null) {
			annotation.desc = 'L' + newInternalName + ';';
		}
		if (annotation.values != null) {
			int size = annotation.values.size();
			for (int i = 0; i < size; i++) {
				@SuppressWarnings("unused") // We are using the cast as a kind of built-in automatic unit test
				String bitvoid = (String) annotation.values.get(i++);
				remapAnnotationValue(annotation.values.get(i), i, annotation.values, sharedStringBuilder);
			}
		}
	}

	private void remapAnnotations(List<? extends AnnotationNode> annotations, StringBuilder sharedStringBuilder) {
		if (annotations == null) {
			return;
		}
		for (AnnotationNode annotation : annotations) {
			remapAnnotation(annotation, sharedStringBuilder);
		}
	}

	@SuppressWarnings("unchecked")
	private void remapAnnotationValue(Object value, int index, List<Object> values, StringBuilder sharedStringBuilder) {
		if (value instanceof Type) {
			String type = ((Type) value).getDescriptor();
			sharedStringBuilder.setLength(0);
			if (remapSignature(type, sharedStringBuilder)) {
				values.set(index, Type.getType(sharedStringBuilder.toString()));
			}
		} else if (value instanceof String[]) {
			String[] enumvals = (String[]) value;
			String internalName = enumvals[0].substring(1, enumvals[0].length() - 1);
			enumvals[1] = mappings.methods.getOrDefault(new MemberRef(internalName, enumvals[1], enumvals[0]), enumvals[1]);
			String newInternalName = mappings.classes.get(internalName);
			if (newInternalName != null) {
				enumvals[0] = 'L' + newInternalName + ';';
			}
		} else if (value instanceof AnnotationNode) {
			remapAnnotation((AnnotationNode) value, sharedStringBuilder);
		} else if (value instanceof List) {
			List<Object> valueList = (List<Object>) value;
			int size = valueList.size();
			for (int i = 0; i < size; i++) {
				remapAnnotationValue(valueList.get(i), i, valueList, sharedStringBuilder);
			}
		} else {
			// Irrelevant
		}
	}

	private void remapBSMArg(final Object[] bsmArgs, final int index, final StringBuilder sharedStringBuilder) {
		Object bsmArg = bsmArgs[index];
		if (bsmArg instanceof Type) {
			Type type = (Type) bsmArg;
			sharedStringBuilder.setLength(0);

			if (type.getSort() == Type.METHOD) {
				if (remapSignature(type.getDescriptor(), sharedStringBuilder)) {
					bsmArgs[index] = Type.getMethodType(sharedStringBuilder.toString());
				}
			} else if (type.getSort() == Type.OBJECT) {
				String oldVal = type.getInternalName();
				String remappedVal = remapInternalName(oldVal, sharedStringBuilder);
				if (oldVal != remappedVal) { // Instance comparison intended
					bsmArgs[index] = Type.getObjectType(remappedVal);
				}
			} else {
				throw new IllegalArgumentException("Unexpected bsm arg Type sort. Sort = " + type.getSort() + "; type = " + type);
			}
		} else if (bsmArg instanceof Handle) {
			Handle handle = (Handle) bsmArg;
			String oldName = handle.getName();
			String hOwner = handle.getOwner();
			String newName = mappings.methods.getOrDefault(new MemberRef(hOwner, oldName, handle.getDesc()), oldName);
			String newOwner = mappings.classes.get(hOwner);
			boolean modified = oldName != newName;
			if (newOwner != null) {
				hOwner = newOwner;
				modified = true;
			}
			String desc = handle.getDesc();
			sharedStringBuilder.setLength(0);
			if (remapSignature(desc, sharedStringBuilder)) {
				desc = sharedStringBuilder.toString();
				modified = true;
			}
			if (modified) {
				bsmArgs[index] = new Handle(handle.getTag(), hOwner, newName, desc, handle.isInterface());
			}
		} else if (bsmArg instanceof String) {
			// Do nothing. I'm kind of surprised that I built this method modular enough that this was a straightforward fix
		} else {
			throw new IllegalArgumentException("Unexpected bsm arg class at index " + index + " for " + Arrays.toString(bsmArgs) + ". Class is " + bsmArg.getClass().getName());
		}
	}

	private void remapField(String owner, FieldNode field, StringBuilder sharedStringBuilder) {
		field.name = mappings.fields.getOrDefault(new MemberRef(owner, field.name, field.desc), field.name);

		int typeType = field.desc.charAt(0);
		if (typeType == '[' || typeType == 'L') {
			// Remap descriptor
			sharedStringBuilder.setLength(0);
			field.desc = remapSingleDesc(field.desc, sharedStringBuilder);
			// Remap signature
			if (field.signature != null) {
				sharedStringBuilder.setLength(0);
				if (remapSignature(field.signature, sharedStringBuilder)) {
					field.signature = sharedStringBuilder.toString();
				}
			}
		}
	}

	private void remapFrameNode(FrameNode frameNode, StringBuilder sharedStringBuilder) {
		if (frameNode.stack != null) {
			int size = frameNode.stack.size();
			for (int i = 0; i < size; i++) {
				Object o = frameNode.stack.get(i);
				if (o instanceof String) {
					String oldName = (String) o;
					String newName = remapInternalName(oldName, sharedStringBuilder);
					if (oldName != newName) { // instance comparision intended
						frameNode.stack.set(i, newName);
					}
				}
			}
		}
		if (frameNode.local != null) {
			int size = frameNode.local.size();
			for (int i = 0; i < size; i++) {
				Object o = frameNode.local.get(i);
				if (o instanceof String) {
					String oldName = (String) o;
					String newName = remapInternalName(oldName, sharedStringBuilder);
					if (oldName != newName) { // instance comparision intended
						frameNode.local.set(i, newName);
					}
				}
			}
		}
	}

	private String remapInternalName(String internalName, StringBuilder sharedStringBuilder) {
		if (internalName.codePointAt(0) == '[') {
			return remapSingleDesc(internalName, sharedStringBuilder);
		} else {
			String remapped = mappings.classes.get(internalName);
			if (remapped != null) {
				return remapped;
			}
			return internalName;
		}
	}

	private void remapMethod(ClassNode owner, MethodNode method, StringBuilder sharedStringBuilder) {
		method.name = mappings.methods.getOrDefault(new MemberRef(owner.name, method.name, method.desc), method.name);
		for (int i = 0; i < method.exceptions.size(); i++) {
			String newExceptionName = mappings.classes.get(method.exceptions.get(i));
			if (newExceptionName != null) {
				method.exceptions.set(i, newExceptionName);
			}
		}
		remapAnnotations(method.invisibleTypeAnnotations, sharedStringBuilder);
		remapAnnotations(method.invisibleLocalVariableAnnotations, sharedStringBuilder);
		remapAnnotations(method.invisibleAnnotations, sharedStringBuilder);
		remapAnnotations(method.visibleAnnotations, sharedStringBuilder);
		remapAnnotations(method.visibleTypeAnnotations, sharedStringBuilder);
		remapAnnotations(method.visibleLocalVariableAnnotations, sharedStringBuilder);
		if (method.invisibleParameterAnnotations != null) {
			for (List<AnnotationNode> annotations : method.invisibleParameterAnnotations) {
				remapAnnotations(annotations, sharedStringBuilder);
			}
		}
		if (method.localVariables != null) {
			for (LocalVariableNode lvn : method.localVariables) {
				int typeType = lvn.desc.charAt(0);
				boolean isObjectArray = typeType == '[';
				int arrayDimension = 0;
				if (isObjectArray) {
					if (lvn.desc.codePointBefore(lvn.desc.length()) == ';') {
						// calculate depth
						int arrayType;
						do {
							arrayType = lvn.desc.charAt(++arrayDimension);
						} while (arrayType == '[');
					} else {
						isObjectArray = false;
					}
				}
				if (isObjectArray || typeType == 'L') {
					// Remap descriptor
					Type type = Type.getType(lvn.desc);
					String internalName = type.getInternalName();
					String newInternalName = mappings.classes.get(internalName);
					if (newInternalName != null) {
						if (isObjectArray) {
							sharedStringBuilder.setLength(arrayDimension);
							for (int i = 0; i < arrayDimension; i++) {
								sharedStringBuilder.setCharAt(i, '[');
							}
							sharedStringBuilder.append(newInternalName);
							sharedStringBuilder.append(';');
							lvn.desc = sharedStringBuilder.toString();
						} else {
							lvn.desc = 'L' + newInternalName + ';';
						}
					}
					if (lvn.signature != null) {
						sharedStringBuilder.setLength(0);
						if (remapSignature(lvn.signature, sharedStringBuilder)) {
							lvn.signature = sharedStringBuilder.toString();
						}
					}
				}
			}
		}
		for (TryCatchBlockNode catchBlock : method.tryCatchBlocks) {
			if (catchBlock.type != null) {
				String newName = mappings.classes.get(catchBlock.type);
				if (newName != null) {
					catchBlock.type = newName;
				}
			}
			remapAnnotations(catchBlock.visibleTypeAnnotations, sharedStringBuilder);
			remapAnnotations(catchBlock.invisibleTypeAnnotations, sharedStringBuilder);
		}
		sharedStringBuilder.setLength(0);
		if (remapSignature(method.desc, sharedStringBuilder)) {
			// The field signature and method desc system are similar enough that this works;
			method.desc = sharedStringBuilder.toString();
		}
		if (method.signature != null) {
			sharedStringBuilder.setLength(0);
			if (remapSignature(method.signature, sharedStringBuilder)) {
				// Method signature and field signature are also similar enough
				method.signature = sharedStringBuilder.toString();
			}
		}
		// boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
		// Type[] paramTypes = Type.getArgumentTypes(method.desc);
		// method.parameters = new ArrayList<ParameterNode>();
		// if(lvMappings != null) {
		// 	for(int i = 0; i < lvMappings.length; i++) {
		// 		String name = lvMappings[i];
		// 		Type type = getParamType(paramTypes, isStatic, i);
		// 		if(type != null) {
		// 			if(name == null) {
		// 				String desc = (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) ? type.getInternalName() : type.getDescriptor();
		// 				int lastSlash = desc.lastIndexOf('/');
		// 				name = desc.substring(lastSlash + 1, lastSlash + 2).toLowerCase() + desc.substring(lastSlash + 2) + i;
		// 			}
		// 			method.parameters.add(new ParameterNode(name, 0));
		// 		}
		// 	}
		// } else {
		// 	int size = Type.getArgumentsAndReturnSizes(method.desc) >> 2;
		// 	for(int i = 0; i < size; i++) {
		// 		Type type = getParamType(paramTypes, isStatic, i);
		// 		if(type != null) {
		// 			String desc = (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) ? type.getInternalName() : type.getDescriptor();
		// 			int lastSlash = desc.lastIndexOf('/');
		// 			String name = desc.substring(lastSlash + 1, lastSlash + 2).toLowerCase() + (desc.length() > 1 ? desc.substring(lastSlash + 2) : "") + i;
		// 			method.parameters.add(new ParameterNode(name, 0));
		// 		}
		// 	}
		// }
		if (method.annotationDefault != null && !(method.annotationDefault instanceof Number)) {
			// Little cheat to avoid writing the same code twice :)
			List<Object> annotationList = Arrays.asList(method.annotationDefault);
			remapAnnotationValue(method.annotationDefault, 0, annotationList, sharedStringBuilder);
			method.annotationDefault = annotationList.get(0);
		}
		InsnList instructions = method.instructions;
		if (instructions != null && instructions.size() != 0) {
			AbstractInsnNode insn = instructions.getFirst();
			while (insn != null) {
				if (insn instanceof FieldInsnNode) {
					FieldInsnNode instruction = (FieldInsnNode) insn;
					String fieldName = mappings.fields.get(new MemberRef(instruction.owner, instruction.name, instruction.desc));
					if (fieldName != null) {
						instruction.name = fieldName;
					}
					instruction.desc = remapSingleDesc(instruction.desc, sharedStringBuilder);
					instruction.owner = remapInternalName(instruction.owner, sharedStringBuilder);
				} else if (insn instanceof FrameNode) {
					remapFrameNode((FrameNode) insn, sharedStringBuilder);
				} else if (insn instanceof InvokeDynamicInsnNode) {
					InvokeDynamicInsnNode specialisedInsn = (InvokeDynamicInsnNode) insn;
					Object[] bsmArgs = specialisedInsn.bsmArgs;
					int arglen = bsmArgs.length;
					for (int i = 0; i < arglen; i++) {
						remapBSMArg(bsmArgs, i, sharedStringBuilder);
					}
					sharedStringBuilder.setLength(0);
					if (remapSignature(specialisedInsn.desc, sharedStringBuilder)) {
						specialisedInsn.desc = sharedStringBuilder.toString();
					}
				} else if (insn instanceof LdcInsnNode) {
					LdcInsnNode specialisedInsn = (LdcInsnNode) insn;
					if (specialisedInsn.cst instanceof Type) {
						String descString = ((Type) specialisedInsn.cst).getDescriptor();
						String newDescString = remapSingleDesc(descString, sharedStringBuilder);
						if (descString != newDescString) {
							specialisedInsn.cst = Type.getType(newDescString);
						}
					}
				} else if (insn instanceof MethodInsnNode) {
					MethodInsnNode instruction = (MethodInsnNode) insn;
					boolean isArray = instruction.owner.codePointAt(0) == '[';
					if (!isArray) { // Javac sometimes invokes methods on array objects
						instruction.name = mappings.methods.getOrDefault(new MemberRef(instruction.owner, instruction.name, instruction.desc), instruction.name);
						String newOwner = mappings.classes.get(instruction.owner);
						if (newOwner != null) {
							instruction.owner = newOwner;
						}
					} else {
						sharedStringBuilder.setLength(0);
						instruction.owner = remapSingleDesc(instruction.owner, sharedStringBuilder);
					}
					sharedStringBuilder.setLength(0);
					if (remapSignature(instruction.desc, sharedStringBuilder)) {
						instruction.desc = sharedStringBuilder.toString();
					}
				} else if (insn instanceof MultiANewArrayInsnNode) {
					MultiANewArrayInsnNode instruction = (MultiANewArrayInsnNode) insn;
					instruction.desc = remapSingleDesc(instruction.desc, sharedStringBuilder);
				} else if (insn instanceof TypeInsnNode) {
					TypeInsnNode instruction = (TypeInsnNode) insn;
					instruction.desc = remapInternalName(instruction.desc, sharedStringBuilder);
				}
				insn = insn.getNext();
			}
		}
	}
	
	// private Type getParamType(Type[] parameters, boolean isStatic, int lvIndex) {
	// 	int paramIndex = isStatic ? 0 : 1;
	// 	for(int i = 0; i < parameters.length; i++) {
	// 		if(paramIndex == lvIndex) {
	// 			return parameters[i];
	// 		}
	// 		paramIndex += parameters[i].getSize();
	// 	}
	// 	return null;
	// }

	private boolean remapSignature(String signature, StringBuilder out) {
		return remapSignature(out, signature, 0, signature.length());
	}

	private boolean remapSignature(StringBuilder signatureOut, String signature, int start, int end) {
		if (start == end) {
			return false;
		}
		int type = signature.codePointAt(start++);
		switch (type) {
		case 'T':
			// generics type parameter
			// fall-through intended as they are similar enough in format compared to objects
		case 'L':
			// object
			// find the end of the internal name of the object
			int endObject = start;
			while(true) {
				// this will skip a character, but this is not interesting as class names have to be at least 1 character long
				int codepoint = signature.codePointAt(++endObject);
				if (codepoint == ';') {
					String name = signature.substring(start, endObject);
					String newName = mappings.classes.get(name);
					boolean modified = false;
					if (newName != null) {
						name = newName;
						modified = true;
					}
					signatureOut.appendCodePoint(type);
					signatureOut.append(name);
					signatureOut.append(';');
					modified |= remapSignature(signatureOut, signature, ++endObject, end);
					return modified;
				} else if (codepoint == '<') {
					// generics - please no
					// post scriptum: well, that was a bit easier than expected
					int openingBrackets = 1;
					int endGenerics = endObject;
					while(true) {
						codepoint = signature.codePointAt(++endGenerics);
						if (codepoint == '>' ) {
							if (--openingBrackets == 0) {
								break;
							}
						} else if (codepoint == '<') {
							openingBrackets++;
						}
					}
					String name = signature.substring(start, endObject);
					String newName = mappings.classes.get(name);
					boolean modified = false;
					if (newName != null) {
						name = newName;
						modified = true;
					}
					signatureOut.append('L');
					signatureOut.append(name);
					signatureOut.append('<');
					modified |= remapSignature(signatureOut, signature, endObject + 1, endGenerics++);
					signatureOut.append('>');
					// apparently that can be rarely be a '.', don't ask when or why exactly this occours
					signatureOut.appendCodePoint(signature.codePointAt(endGenerics));
					modified |= remapSignature(signatureOut, signature, ++endGenerics, end);
					return modified;
				}
			}
		case '+':
			// idk what this one does - but it appears that it works good just like it does right now
		case '*':
			// wildcard - this can also be read like a regular primitive
			// fall-through intended
		case '(':
		case ')':
			// apparently our method does not break even in these cases, so we will consider them raw primitives
		case '[':
			// array - fall through intended as in this case they behave the same
		default:
			// primitive
			signatureOut.appendCodePoint(type);
			return remapSignature(signatureOut, signature, start, end); // Did not modify the signature - but following operations could
		}
	}

	private String remapSingleDesc(String input, StringBuilder sharedBuilder) {
		int indexofL = input.indexOf('L');
		if (indexofL == -1) {
			return input;
		}
		int length = input.length();
		String internalName = input.substring(indexofL + 1, length - 1);
		String newInternalName = mappings.classes.get(internalName);
		if (newInternalName == null) {
			return input;
		}
		sharedBuilder.setLength(indexofL + 1);
		sharedBuilder.setCharAt(indexofL, 'L');
		while(indexofL != 0) {
			sharedBuilder.setCharAt(--indexofL, '[');
		}
		sharedBuilder.append(newInternalName);
		sharedBuilder.append(';');
		return sharedBuilder.toString();
	}
}
