package com.retailai.dto.importing;

public class ValidationErrorDto {
    private int row;
    private String field;
    private String message;

    public ValidationErrorDto() {}

    public ValidationErrorDto(int row, String field, String message) {
        this.row = row;
        this.field = field;
        this.message = message;
    }

    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
