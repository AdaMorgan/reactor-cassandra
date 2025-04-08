package com.datastax.test.action;

public enum Level
{
    ANY(0x0000),
    ONE(0x0001);

    public final int code;
    /**
     *                      0x0000    ANY
     *                      0x0001    ONE
     *                      0x0002    TWO
     *                      0x0003    THREE
     *                      0x0004    QUORUM
     *                      0x0005    ALL
     *                      0x0006    LOCAL_QUORUM
     *                      0x0007    EACH_QUORUM
     *                      0x0008    SERIAL
     *                      0x0009    LOCAL_SERIAL
     *                      0x000A    LOCAL_ONE
     * @return
     */

    Level(final int code)
    {
        this.code = code;
    }

    public int getCode()
    {
        return this.code;
    }
}
