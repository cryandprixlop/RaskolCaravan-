package ru.raskol.caravan.model;

import java.util.UUID;

/** Активный караван: в пути. */
public final class Caravan {
    public String id;               // случайный UUID
    public String nation1;          // имя королевства инициатора
    public String nation2;          // имя королевства партнёра
    public String king1;            // имя короля инициатора
    public String king2;            // имя короля партнёра
    public UUID entityId;           // UUID ArmorStand NPC
    public double deposit1;         // вложение королевства 1
    public double deposit2;         // вложение королевства 2
    public double percent1;         // % прибыли королевству 1
    public double percent2;         // % прибыли королевству 2
    public int guards;              // стражников нанято
    public double hp;               // текущее HP
    public double maxHp;            // макс HP
    public long startedAtEpoch;     // когда стартовал
    public long arriveAtEpoch;      // когда должен прибыть
    public double startX, startY, startZ;
    public double endX, endY, endZ;
    public String world;
}
