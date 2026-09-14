package com.xb.agentscope.controller.demo08;

public class Movie {

    private String title;
    private String director;
    private int year;
    private double rating;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    @Override
    public String toString() {
        return "Movie{title='" + title + "', director='" + director
                + "', year=" + year + ", rating=" + rating + '}';
    }
}