package org.mcphackers.launchwrapper.tweak.injection.vanilla;

import static org.mcphackers.launchwrapper.util.asm.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.protocol.skin.Skin;
import org.mcphackers.launchwrapper.protocol.skin.SkinRequests;
import org.mcphackers.launchwrapper.protocol.skin.SkinTexture;
import org.mcphackers.launchwrapper.tweak.injection.InjectionWithContext;
import org.mcphackers.launchwrapper.tweak.injection.MinecraftGetter;
import org.mcphackers.launchwrapper.util.Base64;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.launchwrapper.util.asm.NodeHelper;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class OfflineSkins extends InjectionWithContext<MinecraftGetter> {
	public OfflineSkins(MinecraftGetter storage) {
		super(storage);
	}

	public String name() {
		return "Offline skins";
	}

	public boolean required() {
		return false;
	}

	public boolean apply(ClassNodeSource source, LaunchConfig config) {
		if (config.disableSkinFix.get()) {
			return false;
		}
		ClassNode mc = context.getMinecraft();
		ClassNode mcSessionService = source.getClass("com/mojang/authlib/minecraft/MinecraftSessionService");
		if (mc == null || mcSessionService == null) {
			return false;
		}
		for (MethodNode m : NodeHelper.getConstructors(mc)) {
			AbstractInsnNode insn = getFirst(m.instructions);
			for (; insn != null; insn = nextInsn(insn)) {
				if (insn.getOpcode() != INVOKEVIRTUAL && insn.getOpcode() != INVOKESPECIAL) {
					continue;
				}
				MethodInsnNode mInsn = (MethodInsnNode)insn;
				// authlib isn't obfuscated. We can rely on hardcoded names
				if (compareInsn(mInsn, -1, "com/mojang/authlib/yggdrasil/YggdrasilAuthenticationService", "createMinecraftSessionService", "()Lcom/mojang/authlib/minecraft/MinecraftSessionService;")) {
					InsnList insns;
					ClassNode sessionService = NodeHelper.newClass(source, ACC_PUBLIC, "org/mcphackers/launchwrapper/inject/SessionService", "java/lang/Object", new String[] { "com/mojang/authlib/minecraft/MinecraftSessionService" }, true);
					FieldNode dispatcher = NodeHelper.newField(sessionService, ACC_PRIVATE, "dispatcher", "Lcom/mojang/authlib/minecraft/MinecraftSessionService;", null, true);

					MethodNode init = NodeHelper.newMethod(sessionService, ACC_PUBLIC,
														   "<init>", "(Lcom/mojang/authlib/minecraft/MinecraftSessionService;)V", null, null, false);
					insns = init.instructions;
					insns.add(new VarInsnNode(ALOAD, 0));
					insns.add(new MethodInsnNode(INVOKESPECIAL, sessionService.superName, "<init>", "()V"));
					insns.add(new VarInsnNode(ALOAD, 0));
					insns.add(new VarInsnNode(ALOAD, 1));
					insns.add(new FieldInsnNode(PUTFIELD, sessionService.name, dispatcher.name, dispatcher.desc));
					insns.add(new InsnNode(RETURN));

					MethodNode wrap = NodeHelper.newMethod(sessionService, ACC_PUBLIC | ACC_STATIC,
														   "wrapSessionService", "(Lcom/mojang/authlib/minecraft/MinecraftSessionService;)Lcom/mojang/authlib/minecraft/MinecraftSessionService;", null, null, true);
					insns = wrap.instructions;
					insns.add(new TypeInsnNode(NEW, sessionService.name));
					insns.add(new InsnNode(DUP));
					insns.add(new VarInsnNode(ALOAD, 0));
					insns.add(new MethodInsnNode(INVOKESPECIAL, sessionService.name, "<init>", init.desc));
					insns.add(new InsnNode(ARETURN));

					for (MethodNode impl : mcSessionService.methods) {
						MethodNode implMethod = NodeHelper.newMethod(sessionService, ACC_PUBLIC,
																	 impl.name, impl.desc, impl.signature, impl.exceptions.toArray(new String[0]), false);
						insns = implMethod.instructions;
						if (impl.name.equals("getTextures") && impl.desc.equals("(Lcom/mojang/authlib/GameProfile;Z)Ljava/util/Map;")) {
							insns.add(new VarInsnNode(ALOAD, 1));
							insns.add(pushIdAndName());
							insns.add(getTextures(source, false));
							continue;
						}
						if (impl.name.equals("getTextures") && impl.desc.equals("(Lcom/mojang/authlib/GameProfile;)Lcom/mojang/authlib/minecraft/MinecraftProfileTextures;")) {
							insns.add(new VarInsnNode(ALOAD, 1));
							insns.add(pushIdAndName());
							insns.add(getTextures(source, true));
							continue;
						}
						if (impl.name.equals("getPackedTextures") && impl.desc.equals("(Lcom/mojang/authlib/GameProfile;)Lcom/mojang/authlib/properties/Property;")) {
							insns.add(new TypeInsnNode(NEW, "com/mojang/authlib/properties/Property"));
							insns.add(new InsnNode(DUP));
							insns.add(new LdcInsnNode("textures"));
							insns.add(new VarInsnNode(ALOAD, 1));
							insns.add(pushIdAndName());
							insns.add(new MethodInsnNode(INVOKESTATIC, "org/mcphackers/launchwrapper/tweak/injection/vanilla/OfflineSkins", "getTexturesBase64", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"));
							insns.add(new MethodInsnNode(INVOKESPECIAL, "com/mojang/authlib/properties/Property", "<init>", "(Ljava/lang/String;Ljava/lang/String;)V"));
							insns.add(new InsnNode(ARETURN));
							continue;
						}
						if (impl.name.equals("unpackTextures") && impl.desc.equals("(Lcom/mojang/authlib/properties/Property;)Lcom/mojang/authlib/minecraft/MinecraftProfileTextures;")) {
							insns.add(new VarInsnNode(ALOAD, 1));
							insns.add(new MethodInsnNode(INVOKEVIRTUAL, "com/mojang/authlib/properties/Property", "value", "()Ljava/lang/String;"));
							insns.add(new MethodInsnNode(INVOKESTATIC, "org/mcphackers/launchwrapper/tweak/injection/vanilla/OfflineSkins", "unpackProfileBase64", "(Ljava/lang/String;)[Ljava/lang/String;"));
							insns.add(new InsnNode(DUP));
							insns.add(new InsnNode(ICONST_0));
							insns.add(new InsnNode(AALOAD));
							insns.add(new InsnNode(SWAP));
							insns.add(new InsnNode(ICONST_1));
							insns.add(new InsnNode(AALOAD));
							insns.add(getTextures(source, true));
							continue;
						}
						int i = 1;
						insns.add(new VarInsnNode(ALOAD, 0));
						insns.add(new FieldInsnNode(GETFIELD, sessionService.name, dispatcher.name, dispatcher.desc));
						for (Type type : Type.getArgumentTypes(impl.desc)) {
							switch (type.getSort()) {
								case Type.BOOLEAN:
								case Type.BYTE:
								case Type.CHAR:
								case Type.SHORT:
								case Type.INT:
									insns.add(new VarInsnNode(ILOAD, i));
									break;
								case Type.LONG:
									insns.add(new VarInsnNode(LLOAD, i));
									break;
								case Type.FLOAT:
									insns.add(new VarInsnNode(FLOAD, i));
									break;
								case Type.DOUBLE:
									insns.add(new VarInsnNode(DLOAD, i));
									break;
								case Type.ARRAY:
								case Type.OBJECT:
									insns.add(new VarInsnNode(ALOAD, i));
									break;
								default:
									return false;
							}
							i += type.getSize();
						}
						insns.add(new MethodInsnNode(INVOKEINTERFACE, "com/mojang/authlib/minecraft/MinecraftSessionService", impl.name, impl.desc));
						switch (Type.getReturnType(impl.desc).getSort()) {
							case Type.BOOLEAN:
							case Type.BYTE:
							case Type.CHAR:
							case Type.SHORT:
							case Type.INT:
								insns.add(new InsnNode(IRETURN));
								break;
							case Type.LONG:
								insns.add(new InsnNode(LRETURN));
								break;
							case Type.FLOAT:
								insns.add(new InsnNode(FRETURN));
								break;
							case Type.DOUBLE:
								insns.add(new InsnNode(DRETURN));
								break;
							case Type.ARRAY:
							case Type.OBJECT:
								insns.add(new InsnNode(ARETURN));
								break;
							case Type.VOID:
								insns.add(new InsnNode(RETURN));
								break;
							default:
								return false;
						}
					}

					source.overrideClass(sessionService);

					m.instructions.insert(insn, new MethodInsnNode(INVOKESTATIC, sessionService.name, wrap.name, wrap.desc));
					source.overrideClass(mc);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Helper methods
	 */
	public static Map<SkinTexture, Skin> getTexturesMap(String id, String username) {
		SkinRequests skins = SkinRequests.getInstance();
		Map<SkinTexture, Skin> map = new HashMap<SkinTexture, Skin>();
		for (SkinTexture skinTex : SkinTexture.values()) {
			Skin skin = skins.downloadSkin(id, username, skinTex);
			if (skin != null) {
				map.put(skinTex, skin);
			}
		}
		return map;
	}

	public static String[] unpackProfileBase64(String base64) {
		String[] uuidAndName = new String[2];
		try {
			JSONObject jsonObject = new JSONObject(new String(Base64.decode(base64), "UTF-8"));
			uuidAndName[0] = jsonObject.getString("profileId");
			uuidAndName[1] = jsonObject.getString("profileName");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return uuidAndName;
	}

	public static String getTexturesBase64(String id, String username) {
		SkinRequests skins = SkinRequests.getInstance();
		JSONObject json = new JSONObject();
		json.put("profileId", id);
		json.put("profileName", username);
		JSONObject textures = new JSONObject();
		json.put("textures", textures);
		for (SkinTexture skinTex : SkinTexture.values()) {
			Skin skin = skins.downloadSkin(id, username, skinTex);
			if (skin == null) {
				continue;
			}
			JSONObject texture = new JSONObject();
			texture.put("url", skin.getURL());
			textures.put(skinTex.name(), texture);
		}
		return Base64.encodeBytes(json.toString().getBytes());
	}
	/*
	 * Helper methods end
	 */

	private static InsnList pushIdAndName() {
		InsnList insns = new InsnList();
		insns.add(new InsnNode(DUP));
		insns.add(new MethodInsnNode(INVOKEVIRTUAL, "com/mojang/authlib/GameProfile", "getId", "()Ljava/util/UUID;"));
		insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/util/UUID", "toString", "()Ljava/lang/String;"));
		insns.add(new LdcInsnNode("-"));
		insns.add(new LdcInsnNode(""));
		insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "replace", "(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;"));
		insns.add(new InsnNode(SWAP));
		insns.add(new MethodInsnNode(INVOKEVIRTUAL, "com/mojang/authlib/GameProfile", "getName", "()Ljava/lang/String;"));
		return insns;
	}

	private static InsnList getTextures(ClassNodeSource source, boolean retRecordClass) {
		int mapIndex = 2;
		int mapIndex2 = 3;
		int valuesIndex = 4;
		int valuesIndex2 = 5;
		int iIndex = 6;
		InsnList insns = new InsnList();
		// stack: String id, String username
		insns.add(new MethodInsnNode(INVOKESTATIC, "org/mcphackers/launchwrapper/tweak/injection/vanilla/OfflineSkins", "getTexturesMap", "(Ljava/lang/String;Ljava/lang/String;)Ljava/util/Map;"));
		insns.add(new VarInsnNode(ASTORE, mapIndex2));

		insns.add(new TypeInsnNode(NEW, "java/util/HashMap"));
		insns.add(new InsnNode(DUP));
		insns.add(new MethodInsnNode(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V"));
		insns.add(new VarInsnNode(ASTORE, mapIndex));

		insns.add(new InsnNode(ICONST_0));
		insns.add(new VarInsnNode(ISTORE, iIndex));

		insns.add(new MethodInsnNode(INVOKESTATIC, "com/mojang/authlib/minecraft/MinecraftProfileTexture$Type", "values", "()[Lcom/mojang/authlib/minecraft/MinecraftProfileTexture$Type;"));
		insns.add(new VarInsnNode(ASTORE, valuesIndex));

		insns.add(new MethodInsnNode(INVOKESTATIC, "org/mcphackers/launchwrapper/protocol/skin/SkinTexture", "values", "()[Lorg/mcphackers/launchwrapper/protocol/skin/SkinTexture;"));
		insns.add(new VarInsnNode(ASTORE, valuesIndex2));

		LabelNode endLoop = new LabelNode();
		LabelNode startLoop = new LabelNode();
		insns.add(startLoop);
		insns.add(new VarInsnNode(ILOAD, iIndex));
		insns.add(new VarInsnNode(ALOAD, valuesIndex));
		insns.add(new InsnNode(ARRAYLENGTH));
		insns.add(new JumpInsnNode(IF_ICMPGE, endLoop));

		insns.add(new VarInsnNode(ALOAD, mapIndex));
		insns.add(new VarInsnNode(ALOAD, mapIndex2));
		insns.add(new VarInsnNode(ALOAD, valuesIndex2));
		insns.add(new VarInsnNode(ILOAD, iIndex));
		insns.add(new InsnNode(AALOAD));
		insns.add(new MethodInsnNode(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;"));
		insns.add(new InsnNode(DUP));
		// stack: Map, Skin, Skin
		LabelNode isNull = new LabelNode();
		insns.add(new JumpInsnNode(IFNULL, isNull));
		// stack: Map, Skin
		insns.add(new TypeInsnNode(CHECKCAST, "org/mcphackers/launchwrapper/protocol/skin/Skin"));
		insns.add(new VarInsnNode(ALOAD, valuesIndex));
		insns.add(new VarInsnNode(ILOAD, iIndex));
		insns.add(new InsnNode(AALOAD));
		insns.add(new InsnNode(SWAP));
		insns.add(new TypeInsnNode(NEW, "com/mojang/authlib/minecraft/MinecraftProfileTexture"));
		insns.add(new InsnNode(DUP_X1));
		insns.add(new InsnNode(SWAP));
		insns.add(new InsnNode(DUP));
		// stack: Map, MinecraftProfileTexture.Type, MinecraftProfileTexture, MinecraftProfileTexture, Skin, Skin
		insns.add(new MethodInsnNode(INVOKEINTERFACE, "org/mcphackers/launchwrapper/protocol/skin/Skin", "getURL", "()Ljava/lang/String;"));
		insns.add(new InsnNode(SWAP));
		// stack: Map, MinecraftProfileTexture.Type, MinecraftProfileTexture, MinecraftProfileTexture, String, Skin
		ClassNode mcSkinTexture = source.getClass("com/mojang/authlib/minecraft/MinecraftProfileTexture");
		boolean hasModels = NodeHelper.getMethod(mcSkinTexture, "<init>", "(Ljava/lang/String;Ljava/util/Map;)V") != null;
		if (hasModels) {
			LabelNode pop = new LabelNode();
			insns.add(new VarInsnNode(ILOAD, iIndex));
			insns.add(new JumpInsnNode(IFNE, pop));
			LabelNode pushNull = new LabelNode();
			insns.add(new MethodInsnNode(INVOKEINTERFACE, "org/mcphackers/launchwrapper/protocol/skin/Skin", "isSlim", "()Z"));
			insns.add(new JumpInsnNode(IFEQ, pushNull));
			LabelNode invokeInit = new LabelNode();
			insns.add(new LdcInsnNode("model"));
			insns.add(new LdcInsnNode("slim"));
			insns.add(new MethodInsnNode(INVOKESTATIC, "java/util/Collections", "singletonMap", "(Ljava/lang/String;Ljava/lang/String;)Ljava/util/Map;"));
			insns.add(new JumpInsnNode(GOTO, invokeInit));
			insns.add(pop);
			insns.add(new InsnNode(POP));
			insns.add(pushNull);
			insns.add(new InsnNode(ACONST_NULL));
			insns.add(invokeInit);
			insns.add(new MethodInsnNode(INVOKESPECIAL, "com/mojang/authlib/minecraft/MinecraftProfileTexture", "<init>", "(Ljava/lang/String;Ljava/util/Map;)V"));
		} else {
			insns.add(new InsnNode(POP));
			insns.add(new MethodInsnNode(INVOKESPECIAL, "com/mojang/authlib/minecraft/MinecraftProfileTexture", "<init>", "(Ljava/lang/String;)V"));
		}
		insns.add(new MethodInsnNode(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"));
		insns.add(new InsnNode(POP));
		insns.add(new IincInsnNode(iIndex, 1));
		insns.add(new JumpInsnNode(GOTO, startLoop));
		insns.add(isNull);
		insns.add(new InsnNode(POP2));
		insns.add(new IincInsnNode(iIndex, 1));
		insns.add(new JumpInsnNode(GOTO, startLoop));
		insns.add(endLoop);

		if (retRecordClass) {
			insns.add(new TypeInsnNode(NEW, "com/mojang/authlib/minecraft/MinecraftProfileTextures"));
			insns.add(new InsnNode(DUP));
			insns.add(new VarInsnNode(ALOAD, mapIndex));
			insns.add(new FieldInsnNode(GETSTATIC, "com/mojang/authlib/minecraft/MinecraftProfileTexture$Type", "SKIN", "Lcom/mojang/authlib/minecraft/MinecraftProfileTexture$Type;"));
			insns.add(new MethodInsnNode(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;"));
			insns.add(new TypeInsnNode(CHECKCAST, "com/mojang/authlib/minecraft/MinecraftProfileTexture"));
			insns.add(new VarInsnNode(ALOAD, mapIndex));
			insns.add(new FieldInsnNode(GETSTATIC, "com/mojang/authlib/minecraft/MinecraftProfileTexture$Type", "CAPE", "Lcom/mojang/authlib/minecraft/MinecraftProfileTexture$Type;"));
			insns.add(new MethodInsnNode(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;"));
			insns.add(new TypeInsnNode(CHECKCAST, "com/mojang/authlib/minecraft/MinecraftProfileTexture"));
			insns.add(new VarInsnNode(ALOAD, mapIndex));
			insns.add(new FieldInsnNode(GETSTATIC, "com/mojang/authlib/minecraft/MinecraftProfileTexture$Type", "ELYTRA", "Lcom/mojang/authlib/minecraft/MinecraftProfileTexture$Type;"));
			insns.add(new MethodInsnNode(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;"));
			insns.add(new TypeInsnNode(CHECKCAST, "com/mojang/authlib/minecraft/MinecraftProfileTexture"));
			insns.add(new FieldInsnNode(GETSTATIC, "com/mojang/authlib/SignatureState", "SIGNED", "Lcom/mojang/authlib/SignatureState;"));
			insns.add(new MethodInsnNode(INVOKESPECIAL, "com/mojang/authlib/minecraft/MinecraftProfileTextures", "<init>", "(Lcom/mojang/authlib/minecraft/MinecraftProfileTexture;Lcom/mojang/authlib/minecraft/MinecraftProfileTexture;Lcom/mojang/authlib/minecraft/MinecraftProfileTexture;Lcom/mojang/authlib/SignatureState;)V"));
		} else {
			insns.add(new VarInsnNode(ALOAD, mapIndex));
		}
		insns.add(new InsnNode(ARETURN));
		return insns;
	}
}
