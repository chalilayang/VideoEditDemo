package com.chalilayang.mediaextractordemo.Utils;

import android.text.TextUtils;

import com.chalilayang.mediaextractordemo.entities.SrtEntity;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;
import com.googlecode.mp4parser.authoring.tracks.TextTrackImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by chalilayang on 2016/11/18.
 */

public class Mp4Parser {

    public static double[] startTrim(String srcpath, String despath, long startMs, long endMs)
            throws IOException {

        startMs = startMs / 1000;
        endMs = endMs / 1000;
        if (TextUtils.isEmpty(srcpath) || TextUtils.isEmpty(despath)) {
            return null;
        }
        File src = new File(srcpath);
        if (!src.exists() || !src.isFile()) {
            return null;
        }
        Movie movie = MovieCreator.build(src.getAbsolutePath());
        List<Track> tracks = movie.getTracks();
        movie.setTracks(new LinkedList<Track>());
        double startTime = startMs / 1000;
        double endTime = endMs / 1000;
        boolean timeCorrected = false;
        // Here we try to find a track that has sync samples. Since we can only start decoding
        // at such a sample we SHOULD make sure that the start of the new fragment is exactly
        // such a frame
        for (Track track : tracks) {
            if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
                if (timeCorrected) {
                    throw new RuntimeException("The startTime has already been corrected by " +
                            "another track with SyncSample. Not Supported.");
                }
                startTime = correctTimeToSyncSample(track, startTime);
                endTime = correctTimeToSyncSample(track, endTime);
                timeCorrected = true;
            }
        }
        for (Track track : tracks) {
            long currentSample = 0;
            double currentTime = 0;
            long startSample = -1;
            long endSample = -1;
            for (int i = 0, count = track.getDecodingTimeEntries().size(); i < count; i++) {
                TimeToSampleBox.Entry entry = track.getDecodingTimeEntries().get(i);
                for (int j = 0; j < entry.getCount(); j++) {
                    if (currentTime <= startTime) {
                        startSample = currentSample;
                    }
                    if (currentTime <= endTime) {
                        endSample = currentSample;
                    } else {
                        break;
                    }
                    currentTime += (double) entry.getDelta() / (double) track.getTrackMetaData()
                            .getTimescale();
                    currentSample++;
                }
            }
            movie.addTrack(new CroppedTrack(track, startSample, endSample));
        }

        Container container = new DefaultMp4Builder().build(movie);

        File dst = new File(despath);
        if (dst.exists()) {
            dst.delete();
        }
        dst.createNewFile();

        FileOutputStream fos = new FileOutputStream(dst);
        FileChannel fc = fos.getChannel();
        container.writeContainer(fc);
        fc.close();
        fos.close();
        double[] doubleArray = new double[2];
        doubleArray[0] = startTime;
        doubleArray[1] = endTime;
        return doubleArray;
    }

    public static void addTextTrack(String srcpath, String destpath, List<SrtEntity> entities) {
        if (TextUtils.isEmpty(srcpath) || TextUtils.isEmpty(destpath)) {
            return;
        }
        File src = new File(srcpath);
        if (!src.exists() || !src.isFile()) {
            return;
        }
        try {
            Movie countVideo = MovieCreator.build(srcpath);

            TextTrackImpl subTitleEng = new TextTrackImpl();
            subTitleEng.getTrackMetaData().setLanguage("eng");
            List<TextTrackImpl.Line> ls = subTitleEng.getSubs();
            for (int index = 0, count = entities.size(); index < count; index++) {
                SrtEntity tmp = entities.get(index);
                ls.add(new TextTrackImpl.Line(tmp.start, tmp.end, tmp.text));
            }
            countVideo.addTrack(subTitleEng);

            Container container = new DefaultMp4Builder().build(countVideo);
            FileOutputStream fos = new FileOutputStream(destpath);
            FileChannel channel = fos.getChannel();
            container.writeContainer(channel);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    private static double correctTimeToSyncSample(Track track, double cutHere) {
        double[] timeOfSyncSamples = new double[track.getSyncSamples().length];
        long currentSample = 0;
        double currentTime = 0;
        for (int i = 0, count = track.getDecodingTimeEntries().size(); i < count; i++) {
            TimeToSampleBox.Entry entry = track.getDecodingTimeEntries().get(i);
            for (int j = 0; j < entry.getCount(); j++) {
                int index = Arrays.binarySearch(track.getSyncSamples(), currentSample + 1);
                if (index >= 0) {
                    // samples always start with 1 but we start with zero therefore +1
                    timeOfSyncSamples[index] = currentTime;
                }
                currentTime += (double) entry.getDelta() / (double) track.getTrackMetaData()
                        .getTimescale();
                currentSample++;
            }
        }
        double previous = 0;
        for (double timeOfSyncSample : timeOfSyncSamples) {
            if (timeOfSyncSample > cutHere) {
                double tmp1 = Math.abs(timeOfSyncSample - cutHere);
                double tmp2 = Math.abs(previous - cutHere);
                return tmp1 <= tmp2 ? timeOfSyncSample : previous;
            }
            previous = timeOfSyncSample;
        }
        return timeOfSyncSamples[timeOfSyncSamples.length - 1];
    }

    public static void appendVideo(String[] videos, String dstvideo) throws IOException {
        Movie[] inMovies = new Movie[videos.length];
        int index = 0;
        for (String video : videos) {
            inMovies[index] = MovieCreator.build(video);
            index++;
        }
        List<Track> videoTracks = new LinkedList<Track>();
        List<Track> audioTracks = new LinkedList<Track>();
        for (Movie m : inMovies) {
            for (Track t : m.getTracks()) {
                if (t.getHandler().equals("soun")) {
                    audioTracks.add(t);
                }
                if (t.getHandler().equals("vide")) {
                    videoTracks.add(t);
                }
            }
        }

        Movie result = new Movie();

//        if (audioTracks.size() > 0) {
//            result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
//        }
        if (videoTracks.size() > 0) {
            result.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
        }
        Container out = new DefaultMp4Builder().build(result);
        FileChannel fc = new RandomAccessFile(String.format(dstvideo), "rw").getChannel();
        out.writeContainer(fc);
        fc.close();
    }
}
