package ru.raskol.caravan.model;

/** Оффер каравана: инициатор -> партнёр. */
public final class Offer {
    public String id;
    public String fromNation;
    public String toNation;
    public String fromKing;
    public String toKing;
    public double deposit1;
    public double deposit2;
    public double percent1;
    public double percent2;
    public int guards;
    public long expiresAtEpoch;
}
