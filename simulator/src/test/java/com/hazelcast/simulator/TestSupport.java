package com.hazelcast.simulator;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import static com.hazelcast.simulator.utils.FileUtils.delete;

public class TestSupport {

    public static void deleteGeneratedRunners() {
        File[] files = new File(System.getProperty("user.dir")).listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            String name = file.getName();
            if (name.endsWith(".java") || name.endsWith(".class")) {
                if (name.contains("Runner")) {
                    delete(file);
                }
            }
        }
    }

    public static <E> Future<E> spawn(Callable<E> e) {
        FutureTask<E> task = new FutureTask<E>(e);
        Thread thread = new Thread(task);
        thread.start();
        return task;
    }
}