package com.mego.fizoalarm.pojo;


import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import com.mego.fizoalarm.pojo.challenges.Challenge;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Alarm implements Serializable,Cloneable,Comparable<Alarm> {

    private int id;
    @JsonAdapter(LocalDateJsonAdapter.class)
    private LocalDate date;
    @JsonAdapter(LocalTimeJsonAdapter.class)
    private LocalTime time;
    private List<DayOfWeek> repeat_days = new ArrayList<>();
    private String label;
    private boolean enabled;
    private boolean snooze = true;
    private int snooze_repeat_count = 0;
    private int snooze_in_minutes = 5;
    private RingtoneData ringtoneData ;
    private String videoFile ;
    private float volume = 1f;
    private boolean vibration = true;
    private boolean flash_light ;
    @JsonAdapter(ChallengeConfigJsonAdapter.class)
    private Challenge challenge;

    //for clone method
    public Alarm() {

    }

    public Alarm(LocalDate date, LocalTime time, RingtoneData ringtoneData) {
        this.date = date;
        this.time = time;
        this.ringtoneData = ringtoneData;
    }

    public static Alarm fromJson(String jsonString) {
        Gson gson = new Gson();
        return gson.fromJson( jsonString, Alarm.class);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate alarmDate) {
        this.date = alarmDate;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime alarmTime) {
        this.time = alarmTime;
    }

    public List<DayOfWeek> getRepeat_days() {
        return repeat_days;
    }

    public void setRepeat_days(List<DayOfWeek> repeat_days) {
        this.repeat_days = repeat_days;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean getSnooze() {
        return snooze;
    }

    public void setSnooze(boolean snooze) {
        this.snooze = snooze;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getSnooze_repeat_count() {
        return snooze_repeat_count;
    }

    public void setSnooze_repeat_count(int snooze_repeat_count) {
        this.snooze_repeat_count = snooze_repeat_count;
    }

    public int getSnooze_in_minutes() {
        return snooze_in_minutes;
    }

    public void setSnooze_in_minutes(int snooze_in_minutes) {
        this.snooze_in_minutes = snooze_in_minutes;
    }

    public RingtoneData getRingtoneData() {
        return ringtoneData;
    }

    public void setRingtoneData(RingtoneData ringtoneData) {
        this.ringtoneData = ringtoneData;
    }

    public String getVideoFile() {
        return videoFile;
    }

    public void setVideoFile(String videoFile) {
        this.videoFile = videoFile;
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public boolean isVibration() {
        return vibration;
    }

    public void setVibration(boolean vibration) {
        this.vibration = vibration;
    }

    public boolean isFlash_light() {
        return flash_light;
    }

    public void setFlash_light(boolean flash_light) {
        this.flash_light = flash_light;
    }

    public Challenge getChallenge() { return challenge; }

    public void setChallenge(Challenge challenge) {
        this.challenge = challenge;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public void changeDateToNextRepeat() {

        LocalDateTime now = LocalDateTime.now();

        if ( repeat_days.size() == 0)
            return ;

        //repeat alarm start from today
        if (repeat_days.contains(now.getDayOfWeek()) && now.toLocalTime().isBefore(time) ) {
            setDate(now.toLocalDate());
            return ;
        }

        Collections.sort(repeat_days);

        DayOfWeek nextDay = null;

        for (DayOfWeek day : getRepeat_days())
            if (now.getDayOfWeek().getValue() < day.getValue()) {
                nextDay = day;
                break;
            }

        if (nextDay == null)
        nextDay = repeat_days.get(0);

        setDate( now.toLocalDate().with(TemporalAdjusters.next(nextDay)) );

    }

    @Override
    public Alarm clone() {
        Alarm clonedAlarm = new Alarm();

        clonedAlarm.id = this.id;
        clonedAlarm.date = this.date;
        clonedAlarm.time = this.time;
        clonedAlarm.repeat_days.addAll(this.repeat_days);
        clonedAlarm.label = this.label;
        clonedAlarm.enabled = this.enabled;
        clonedAlarm.snooze = this.snooze;
        clonedAlarm.snooze_repeat_count = this.snooze_repeat_count;
        clonedAlarm.snooze_in_minutes = this.snooze_in_minutes;
        clonedAlarm.ringtoneData =
                new RingtoneData(this.ringtoneData.getUri(),this.ringtoneData.getTitle(),this.ringtoneData.getTypeID()) ;
        clonedAlarm.videoFile = this.videoFile;
        clonedAlarm.volume = this.volume;
        clonedAlarm.vibration = this.vibration;
        clonedAlarm.flash_light = this.flash_light;
        if (this.challenge != null)
            clonedAlarm.setChallenge( this.challenge.clone() );

        return clonedAlarm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Alarm anotherAlarm = (Alarm) o;
        return id == anotherAlarm.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public int compareTo(Alarm alarm) {
        if ( this.getTime().equals(alarm.getTime()) )
            return 0;
        boolean isAfter = this.getTime().isAfter(alarm.getTime());
        return isAfter ? 1 : -1 ;
    }
}