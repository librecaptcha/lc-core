package lc.misc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;

public class PngImageWriter {

  static final int DPI = 245;
  static final double INCH_2_CM = 2.54;

  public static void write(ByteArrayOutputStream boas, BufferedImage gridImage) throws IOException {
    final String formatName = "png";
    for (Iterator<ImageWriter> iw = ImageIO.getImageWritersByFormatName(formatName);
        iw.hasNext(); ) {
      ImageWriter writer = iw.next();
      ImageWriteParam writeParam = writer.getDefaultWriteParam();
      ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
      IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);
      if (metadata.isReadOnly() || !metadata.isStandardMetadataFormatSupported()) {
        continue;
      }

      setDPIMeta(metadata);

      final ImageOutputStream stream = ImageIO.createImageOutputStream(boas);
      try {
        writer.setOutput(stream);
        writer.write(metadata, new IIOImage(gridImage, null, metadata), writeParam);
      } finally {
        stream.close();
      }
      break;
    }
  }

  private static void setDPIMeta(IIOMetadata metadata) throws IIOInvalidTreeException {

    // for PNG, it's dots per millimeter
    double dotsPerMilli = 1.0 * DPI / 10 / INCH_2_CM;

    IIOMetadataNode horiz = new IIOMetadataNode("HorizontalPixelSize");
    horiz.setAttribute("value", Double.toString(dotsPerMilli));

    IIOMetadataNode vert = new IIOMetadataNode("VerticalPixelSize");
    vert.setAttribute("value", Double.toString(dotsPerMilli));

    IIOMetadataNode dim = new IIOMetadataNode("Dimension");
    dim.appendChild(horiz);
    dim.appendChild(vert);

    IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
    root.appendChild(dim);

    metadata.mergeTree("javax_imageio_1.0", root);
  }
}
