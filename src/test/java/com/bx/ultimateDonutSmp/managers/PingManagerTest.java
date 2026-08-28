package com.bx.ultimateDonutSmp.managers;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PingManagerTest {

    @Test
    void nullPlayerReturnsZeroPing() throws Exception {
        // PingManager's constructor registers listeners and starts a task on the live server,
        // which a unit test cannot provide. The null-player contract of getPing does not touch
        // any of that state, so an instance allocated without running the constructor is enough.
        PingManager pingManager = allocateWithoutConstructor();
        assertEquals(0, pingManager.getPing(null));
    }

    private static PingManager allocateWithoutConstructor() throws Exception {
        Field theUnsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) theUnsafeField.get(null);
        return (PingManager) unsafe.allocateInstance(PingManager.class);
    }
}
