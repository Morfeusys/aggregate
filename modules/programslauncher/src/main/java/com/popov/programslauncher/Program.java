package com.popov.programslauncher;

/**
 * Created by popov on 24.03.2016.
 */
public class Program {
    private String name;
    private String pattern;
    private String path;
    public Program(String name, String pattern, String path){
        this.name = name;
        this.pattern = pattern;
        this.path = path;
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
}
