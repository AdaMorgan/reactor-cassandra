import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class CustomLengthFieldBasedFrameDecoder extends LengthFieldBasedFrameDecoder
{
    private static final int MAX_FRAME_LENGTH = 256 * 1024 * 1024; // 256 MB

    public CustomLengthFieldBasedFrameDecoder(int lengthFieldOffset)
    {
        super(MAX_FRAME_LENGTH, lengthFieldOffset + 3, 4, 0, 0, true);
    }
}
