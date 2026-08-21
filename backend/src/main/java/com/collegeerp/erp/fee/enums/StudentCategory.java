package com.collegeerp.erp.fee.enums;

public enum StudentCategory {
    OPEN(0),
    OBC(1),
    SC(2),
    ST(3),
    SBC(4),
    VJNT(5),
    EWS(6),
    OTHER(7);

    private final int displayOrder;

    StudentCategory(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public int displayOrder() {
        return displayOrder;
    }
}
