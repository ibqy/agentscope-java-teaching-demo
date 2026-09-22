package com.xb.agentscope.controller.demo08;

/**
 * Movie - 电影信息数据对象，用于结构化输出演示
 *
 * 作为 Demo08 的结构化输出目标类型，Agent 的 JSON 回复会被自动映射到此 POJO。
 * 学习者应理解：字段名需与 Agent 被提示输出的 JSON key 保持一致。
 *
 * @author ibqy
 */
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