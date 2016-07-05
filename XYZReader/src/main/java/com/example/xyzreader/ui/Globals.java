package com.example.xyzreader.ui;

public class Globals {
    private static Globals instance;

    // Global variables
    private long selectedItemId;

    // Restrict the constructor from being instantiated
    private Globals(){}

    public static synchronized Globals getInstance(){
        if(instance==null){
            instance=new Globals();
        }
        return instance;
    }

    public void setSelectedItemId(long l){
        this.selectedItemId = l;
    }
    public long getSelectedItemId(){
        return this.selectedItemId;
    }

}
