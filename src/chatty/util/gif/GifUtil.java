
package chatty.util.gif;

import chatty.util.gif.GifDecoder.GifImage;
import java.awt.MediaTracker;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.ImageIcon;

/**
 *
 * @author tduva
 */
public class GifUtil {
    
    private static final Logger LOGGER = Logger.getLogger(GifUtil.class.getName());
    
    /**
     * Loads a GIF from the given URL, fixing FPS, or just creates an ImageIcon
     * directly if it's not a valid GIF.
     * 
     * @param url The URL (local or internet) to get the image data from
     * @return The created ImageIcon, or null if an error occured creating the
     * image
     * @throws Exception When an error occured loading the image
     */
    public static ImageIcon getGifFromUrl(URL url) throws Exception {
        try (InputStream input = url.openStream()) {
            // Use readAllBytes() because GifDecoder doesn't handle streams well
            byte[] imageData = readAllBytes(input);
            ImageIcon image = null;
            try {
                image = fixGifFps(imageData);
                image.setDescription("GIF");
            } catch (Exception ex) {
                /**
                 * If not a GIF, or another error occured, just create the image
                 * normally.
                 */
                image = new ImageIcon(imageData);
            }
            
            if (image.getImageLoadStatus() == MediaTracker.ERRORED) {
                return null;
            }
            return image;
        }
    }

    /**
     * Decodes and re-writes the animated GIF with modified FPS. Any delay
     * smaller than 10ms is set to 100ms. Yes, this is kind of ugly, but it
     * worked best with the GIF emotes I tested.
     * 
     * @param imageData
     * @return
     * @throws IOException 
     */
    private static ImageIcon fixGifFps(byte[] imageData) throws IOException {
        final GifImage gif = GifDecoder.read(imageData);
        //System.out.println(hash(imageData));
        if (DONT_FIX.contains(hash(imageData))) {
            /**
             * Don't try to fix some GIFs, which look better originally. This is
             * obviously not ideal, but until a method is found that works well
             * for ALL GIFs, this must do.
             */
            return new ImageIcon(imageData);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(bos)) {
            int delay = gif.getDelay(0) * 10;
            if (delay <= 10) {
                delay = 100;
            }
            GifSequenceWriter w = new GifSequenceWriter(output, gif.getFrame(0).getType(), delay, true);
            for (int i = 0; i < gif.getFrameCount(); i++) {
                //System.out.println(gif.getDelay(i));
                w.writeToSequence(gif.getFrame(i));
            }
            w.close();
        }
        return new ImageIcon(bos.toByteArray());
    }
    
    private static final Set<String> DONT_FIX = new HashSet<>(Arrays.asList(new String[]{
        "0571f6a1a125404f5e6f75ae206381e381c76304", // SourPls
        "52ac1713a09fe3df1b8794d47a326e80c23f06dd", // SourPls
    }));

    /**
     * Read all bytes from the stream into a byte array.
     * 
     * @param input
     * @return
     * @throws IOException 
     */
    private static byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = input.read(buffer, 0, buffer.length)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toByteArray();
    }
    
    private static String hash(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return byteArrayToHexString(md.digest(input));
        } catch (Exception ex) {
            // Fail silently
        }
        return null;
    }
    
    private static String byteArrayToHexString(byte[] b) {
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }
    
}
