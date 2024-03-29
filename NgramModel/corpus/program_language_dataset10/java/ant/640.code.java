package org.apache.tools.ant.types.optional.image;
import java.awt.Color;
public final class ColorMapper {
    private ColorMapper() {
    }
    public static final String COLOR_BLACK = "black";
    public static final String COLOR_BLUE = "blue";
    public static final String COLOR_CYAN = "cyan";
    public static final String COLOR_DARKGRAY = "darkgray";
    public static final String COLOR_GRAY = "gray";
    public static final String COLOR_LIGHTGRAY = "lightgray";
    public static final String COLOR_DARKGREY = "darkgrey";
    public static final String COLOR_GREY = "grey";
    public static final String COLOR_LIGHTGREY = "lightgrey";
    public static final String COLOR_GREEN = "green";
    public static final String COLOR_MAGENTA = "magenta";
    public static final String COLOR_ORANGE = "orange";
    public static final String COLOR_PINK = "pink";
    public static final String COLOR_RED = "red";
    public static final String COLOR_WHITE = "white";
    public static final String COLOR_YELLOW = "yellow";
    public static Color getColorByName(String colorName) {
        if (colorName.equalsIgnoreCase(COLOR_BLACK)) {
            return Color.black;
        } else if (colorName.equalsIgnoreCase(COLOR_BLUE)) {
            return Color.blue;
        } else if (colorName.equalsIgnoreCase(COLOR_CYAN)) {
            return Color.cyan;
        } else if (colorName.equalsIgnoreCase(COLOR_DARKGRAY) || colorName.equalsIgnoreCase(COLOR_DARKGREY)) {
            return Color.darkGray;
        } else if (colorName.equalsIgnoreCase(COLOR_GRAY) || colorName.equalsIgnoreCase(COLOR_GREY)) {
            return Color.gray;
        } else if (colorName.equalsIgnoreCase(COLOR_LIGHTGRAY) || colorName.equalsIgnoreCase(COLOR_LIGHTGREY)) {
            return Color.lightGray;
        } else if (colorName.equalsIgnoreCase(COLOR_GREEN)) {
            return Color.green;
        } else if (colorName.equalsIgnoreCase(COLOR_MAGENTA)) {
            return Color.magenta;
        } else if (colorName.equalsIgnoreCase(COLOR_ORANGE)) {
            return Color.orange;
        } else if (colorName.equalsIgnoreCase(COLOR_PINK)) {
            return Color.pink;
        } else if (colorName.equalsIgnoreCase(COLOR_RED)) {
            return Color.red;
        } else if (colorName.equalsIgnoreCase(COLOR_WHITE)) {
            return Color.white;
        } else if (colorName.equalsIgnoreCase(COLOR_YELLOW)) {
            return Color.yellow;
        }
        return Color.black;
    }
}
