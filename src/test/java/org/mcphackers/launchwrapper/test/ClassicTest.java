package org.mcphackers.launchwrapper.test;

import java.io.File;

import org.junit.Test;

public class ClassicTest extends TweakTest {

    @Test
    public void test() {
        System.out.println(new File(".").getAbsolutePath());
    }
}
