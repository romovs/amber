package haven;

import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;

public class PlayerPosStreamer implements Runnable {

    public void run() {
        FileChannel channel = null;
        Coord lastpos = Coord.z;

        try {
            File f = new File(Config.playerposfile);

            channel = FileChannel.open(f.toPath(),
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE);

            MappedByteBuffer b = channel.map(FileChannel.MapMode.READ_WRITE, 0, 8);
            IntBuffer buf = b.asIntBuffer();

            while (true) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    return;
                }

                Coord pos = LocalMiniMap.plcrel;
                if (pos == null || pos.equals(lastpos))
                    continue;
                lastpos = pos;
                FileLock lock = channel.lock(0, 8, false);
                buf.position(0);
                buf.put(pos.x);
                buf.put(pos.y);
                lock.release();
            }

        } catch (IOException ioe) {
            System.err.println("Unable to open player's position file: " + ioe.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
