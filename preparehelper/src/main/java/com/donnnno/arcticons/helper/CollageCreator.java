package com.donnnno.arcticons.helper;



import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CollageCreator {

    public static void main(String[] args) {
        String rootDir = System.getProperty("user.dir");
        Path rootPath = Paths.get(rootDir);
        String rootDirName = rootPath.getFileName().toString();
        if (rootDirName.equals("preparehelper")) {
            rootDir = "..";
        }
        String xmlFilePath = rootDir + "/app/src/main/res/xml/contributors.xml";

        // Step 1: Extract image URLs from XML
        List<String> imageUrls = extractImageUrls(xmlFilePath);
        if (imageUrls.isEmpty()) {
            System.err.println("No image URLs found in the XML file.");
            return;
        }

        // Step 2: Download images
        List<BufferedImage> images = downloadImages(imageUrls);
        if (images.isEmpty()) {
            System.err.println("No images were downloaded.");
            return;
        }

        // Step 3: Create collage
        int collageWidth = 794;
        int collageHeight = 559;
        BufferedImage collage = createCollage(images, collageWidth, collageHeight);

        // Step 4: Save collage to file
        try {
            ImageIO.write(collage, "png", new File("collage.png"));
            System.out.println("Collage created successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<BufferedImage> downloadImages(List<String> imageUrls) {
        List<BufferedImage> images = new ArrayList<>();

        for (String imageUrl : imageUrls) {
            try {
                URL url = new URL(imageUrl);
                BufferedImage img = ImageIO.read(url);
                if (img != null) {
                    images.add(img);
                    System.out.println("Downloaded image from: " + imageUrl);
                } else {
                    System.err.println("Failed to download image from: " + imageUrl);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return images;
    }

    public static BufferedImage createCollage(List<BufferedImage> images, int collageWidth, int collageHeight) {
        if (images.isEmpty()) {
            throw new IllegalArgumentException("No images to create a collage.");
        }
        BufferedImage collage = new BufferedImage(collageWidth, collageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = collage.createGraphics();

        collageWidth -= 10;
        collageHeight -= 10;

        int numImages = images.size();
        int rows = (int) Math.ceil(Math.sqrt(numImages));
        int cols = (int) Math.ceil((double) numImages / rows);

        int thumbWidth = collageWidth / cols;
        int thumbHeight = collageHeight / rows;

        System.out.println("thumbWidth: " + thumbWidth);
        System.out.println("thumbHeight: " + thumbHeight);


        Random rand = new Random();

        List<Rectangle> positions = new ArrayList<>();
        int x = 10, y = 10;
        for (BufferedImage image : images) {
            BufferedImage resizedImage = resizeImage(image, thumbWidth, thumbHeight);
            int ImageWidth = resizedImage.getWidth();
            int ImageHeight = resizedImage.getHeight();

            Rectangle newRect;
            int attempts = 0;
            boolean useSlightOverlap = false;
            do {
            //int stackingW = collageWidth/100*(attempts/10)/2;
            //int stackingH = collageHeight/100*(attempts/10)/2;
            //int offsetX =  rand.nextInt(collageWidth/2-thumbWidth-stackingW,collageWidth/2+thumbWidth+stackingW); // Random offset to slightly randomize position
            //int offsetY = rand.nextInt(collageHeight/2-thumbWidth-stackingH,collageHeight/2+thumbHeight+stackingH); // Random offset to slightly randomize position
                int offsetX = (thumbWidth - ImageWidth) / 2;
                int offsetY = (thumbHeight - ImageHeight) / 2;
                newRect = new Rectangle(x+offsetX, y+offsetY, ImageWidth, ImageHeight);
                attempts++;

                if (!useSlightOverlap && attempts >= 10000) {
                    useSlightOverlap = true;
                    attempts = 0; // Reset attempts for slight overlap phase
                }

            } while (checkOverlap(newRect, positions, useSlightOverlap) && attempts < 10000);

            if (attempts < 10000) {
                positions.add(newRect);
                g2d.drawImage(resizedImage, newRect.x, newRect.y, null);
            }
            x += thumbWidth;
            if (x >= collageWidth) {
                x = 10;
                y += thumbHeight;
            }
        }

        g2d.dispose();
        return collage;
    }

    public static boolean checkOverlap(Rectangle rect, List<Rectangle> positions, boolean useSlightOverlap) {
        for (Rectangle position : positions) {
            if (!useSlightOverlap && rect.intersects(position)) {
                return true;
            }
            else if (useSlightOverlap) {
                int offset = 20;
                if (rect.intersects(position.x + offset, position.y + offset, position.width - offset, position.height - offset)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // Calculate new dimensions while preserving aspect ratio
        double aspectRatio = (double) originalWidth / originalHeight;
        int newWidth = targetWidth;
        int newHeight = (int) (targetWidth / aspectRatio);

        if (newHeight > targetHeight) {
            newHeight = targetHeight;
            newWidth = (int) (targetHeight * aspectRatio);
        }

        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        return resizedImage;
    }

    public static List<String> extractImageUrls(String xmlFilePath) {
        List<String> imageUrls = new ArrayList<>();

        try {
            File inputFile = new File(xmlFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("contributor");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String imageUrl = eElement.getAttribute("image");
                    if (!imageUrl.isEmpty()) {
                        imageUrls.add(imageUrl);
                        System.out.println("Extracted image URL: " + imageUrl);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return imageUrls;
    }
}
