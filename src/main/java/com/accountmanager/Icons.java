package com.accountmanager;

import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

final class Icons
{
	static final int SIZE = 16;

	static final BufferedImage PANEL_ICON_IMG;
	static final ImageIcon ADD_ICON;
	static final ImageIcon ADD_HOVER_ICON;
	static final ImageIcon LOCK_ICON;
	static final ImageIcon LOGIN_ICON;
	static final ImageIcon LOGIN_HOVER_ICON;
	static final ImageIcon EDIT_ICON;
	static final ImageIcon EDIT_HOVER_ICON;
	static final ImageIcon DELETE_ICON;
	static final ImageIcon DELETE_HOVER_ICON;
	static final ImageIcon TOTP_ICON;

	static
	{
		// Panel icon - user/account symbol
		PANEL_ICON_IMG = createImage(g ->
		{
			g.setColor(ColorScheme.LIGHT_GRAY_COLOR);
			// Head
			g.fillOval(5, 2, 6, 6);
			// Body
			g.fillArc(2, 8, 12, 10, 0, 180);
		});

		// Add icon - plus symbol
		ADD_ICON = createIcon(g ->
		{
			g.setColor(ColorScheme.PROGRESS_COMPLETE_COLOR);
			g.setStroke(new BasicStroke(2));
			g.drawLine(8, 3, 8, 13);
			g.drawLine(3, 8, 13, 8);
		});
		ADD_HOVER_ICON = createHoverIcon(ADD_ICON);

		// Lock icon
		LOCK_ICON = createIcon(32, g ->
		{
			g.setColor(ColorScheme.LIGHT_GRAY_COLOR);
			// Lock body
			g.fillRoundRect(6, 14, 20, 14, 3, 3);
			// Shackle
			g.setStroke(new BasicStroke(3));
			g.drawArc(10, 6, 12, 12, 0, 180);
			// Keyhole
			g.setColor(ColorScheme.DARKER_GRAY_COLOR);
			g.fillOval(14, 18, 4, 4);
			g.fillRect(15, 21, 2, 4);
		});

		// Login icon - arrow pointing right
		LOGIN_ICON = createIcon(g ->
		{
			g.setColor(ColorScheme.PROGRESS_COMPLETE_COLOR);
			g.setStroke(new BasicStroke(2));
			// Arrow shaft
			g.drawLine(3, 8, 11, 8);
			// Arrow head
			g.drawLine(8, 4, 12, 8);
			g.drawLine(8, 12, 12, 8);
		});
		LOGIN_HOVER_ICON = createHoverIcon(LOGIN_ICON);

		// Edit icon - pencil
		EDIT_ICON = createIcon(g ->
		{
			g.setColor(ColorScheme.LIGHT_GRAY_COLOR);
			g.setStroke(new BasicStroke(1.5f));
			// Pencil body
			g.drawLine(3, 13, 11, 5);
			g.drawLine(5, 13, 13, 5);
			// Tip
			g.drawLine(3, 13, 2, 14);
		});
		EDIT_HOVER_ICON = createHoverIcon(EDIT_ICON);

		// Delete icon - X symbol
		DELETE_ICON = createIcon(g ->
		{
			g.setColor(ColorScheme.PROGRESS_ERROR_COLOR);
			g.setStroke(new BasicStroke(2));
			g.drawLine(4, 4, 12, 12);
			g.drawLine(12, 4, 4, 12);
		});
		DELETE_HOVER_ICON = createHoverIcon(DELETE_ICON);

		// TOTP icon - clock/shield
		TOTP_ICON = createIcon(g ->
		{
			g.setColor(new Color(100, 149, 237)); // Cornflower blue
			// Shield shape
			g.fillRoundRect(3, 2, 10, 12, 2, 2);
			g.setColor(ColorScheme.DARKER_GRAY_COLOR);
			// Clock hands
			g.setStroke(new BasicStroke(1.5f));
			g.drawLine(8, 5, 8, 8);
			g.drawLine(8, 8, 10, 10);
		});
	}

	private Icons()
	{
	}

	private static ImageIcon createIcon(IconPainter painter)
	{
		return createIcon(SIZE, painter);
	}

	private static ImageIcon createIcon(int size, IconPainter painter)
	{
		return new ImageIcon(createImage(size, painter));
	}

	private static BufferedImage createImage(IconPainter painter)
	{
		return createImage(SIZE, painter);
	}

	private static BufferedImage createImage(int size, IconPainter painter)
	{
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		painter.paint(g);
		g.dispose();
		return image;
	}

	private static ImageIcon createHoverIcon(ImageIcon original)
	{
		Image img = original.getImage();
		BufferedImage buffered = new BufferedImage(
			img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = buffered.createGraphics();
		g.drawImage(img, 0, 0, null);
		g.dispose();

		// Brighten the image
		for (int y = 0; y < buffered.getHeight(); y++)
		{
			for (int x = 0; x < buffered.getWidth(); x++)
			{
				int rgba = buffered.getRGB(x, y);
				int a = (rgba >> 24) & 0xff;
				int r = Math.min(255, ((rgba >> 16) & 0xff) + 50);
				int g2 = Math.min(255, ((rgba >> 8) & 0xff) + 50);
				int b = Math.min(255, (rgba & 0xff) + 50);
				buffered.setRGB(x, y, (a << 24) | (r << 16) | (g2 << 8) | b);
			}
		}
		return new ImageIcon(buffered);
	}

	@FunctionalInterface
	private interface IconPainter
	{
		void paint(Graphics2D g);
	}
}
