package com.popov.programslauncher;

/**
 * Created by popov on 24.03.2016.
 */
public class Program {
    private String name;
    private String pattern;
    private String path;
    private int[] keyCode;
    public Program(String name, String pattern, String path, int[] keyCode){
        this.name = name;
        this.pattern = pattern;
        this.path = path;
        this.keyCode = keyCode;
    }
    public Program(){
    }



    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int[] getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int[] keyCode) {
        this.keyCode = keyCode;
    }
}
