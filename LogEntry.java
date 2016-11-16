package com.daniel.bikehours;


public class LogEntry {

    long time = 0;
    String location = "";
    int ridingTime = 0;

    public LogEntry(long time, String location, int ridingTime){
        this.time = time;
        this.location = location;
        this.ridingTime = ridingTime;
    }

    public LogEntry(String data){
        String[] table = data.split(";");
        this.time = Long.parseLong(table[0]);
        this.location = table[1];
        this.ridingTime = Integer.parseInt(table[2]);
    }

    public long getTime() { return time; }
    public String getLocation() { return location; }
    public int getRidingTime() { return ridingTime; }

    public String getData(){

        return time+";"+location+";"+ridingTime;

    }
}
