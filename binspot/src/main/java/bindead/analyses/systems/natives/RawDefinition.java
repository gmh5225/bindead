package bindead.analyses.systems.natives;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

class RawDefinition {
  private final ByteBuffer data;

  protected RawDefinition (ByteBuffer data) {
    this.data = data;
  }

  public ByteBufferBackedInputStream wrap() {
    return new ByteBufferBackedInputStream(data);
  }

  public ByteBuffer getData () {
    return data;
  }


  private static class ByteBufferBackedInputStream extends InputStream {
    private final ByteBuffer buf;

    private ByteBufferBackedInputStream (ByteBuffer buf) {
      this.buf = buf;
    }

    @Override
    public synchronized int read () throws IOException {
      if (!buf.hasRemaining()) {
        return -1;
      }
      return buf.get() & 0xFF;
    }

    @Override
    public synchronized int read (byte[] bytes, int off, int len)
        throws IOException {
      if (!buf.hasRemaining()) {
        return -1;
      }

      len = Math.min(len, buf.remaining());
      buf.get(bytes, off, len);
      return len;
    }
  }
}
