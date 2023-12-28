package com.pencil.prescription;

public class CourseModal {
    private int id;
    private int posX, posY;
    private String text;
    private int fontSize;
    private String fontFamily, fontColor;

    public String getText() { return text; }

    public CourseModal(int posX, int posY, String text, int fontSize, String fontFamily, String fontColor) {
        this.posX = posX;
        this.posY = posY;
        this.text = text;
        this.fontSize = fontSize;
        this.fontFamily = fontFamily;
        this.fontColor = fontColor;
    }
}
