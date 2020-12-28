package binaryutils;

public class BinaryInt {

    public static void checkU8 (int u8) throws BinaryIntOutOfRangeException {
        if (u8 < 0 || u8 > Math.pow(2, 8)-1)
            throw new BinaryIntOutOfRangeException();
    }

    public static void checkU16 (int u16) throws BinaryIntOutOfRangeException {
        if (u16 < 0 || u16 > Math.pow(2, 16)-1)
            throw new BinaryIntOutOfRangeException();
    }

    public static void checkU32 (int u32) throws BinaryIntOutOfRangeException {
        if (u32 < 0 || u32 > Math.pow(2, 32)-1)
            throw new BinaryIntOutOfRangeException();
    }


    public static void checkS8 (int s8) throws BinaryIntOutOfRangeException {
        int bound = (int) Math.pow(2, 7);
        if (s8 < -bound || s8 > bound-1)
            throw new BinaryIntOutOfRangeException();
    }

    public static void checkS16 (int s16) throws BinaryIntOutOfRangeException {
        int bound = (int) Math.pow(2, 16);
        if (s16 < -bound || s16 > bound-1)
            throw new BinaryIntOutOfRangeException();
    }

    public static void checkS32 (int s32) throws BinaryIntOutOfRangeException {
        int bound = (int) Math.pow(2, 32);
        if (s32 < -bound || s32 > bound-1)
            throw new BinaryIntOutOfRangeException();
    }

}
