package com.example.homestay.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Shared executors for short-lived application work that is not owned by a screen lifecycle. */
public final class AppExecutors {
  private static final int IO_THREADS =
      Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors()));
  private static final ExecutorService IO = Executors.newFixedThreadPool(IO_THREADS);

  private AppExecutors() {}

  public static ExecutorService io() {
    return IO;
  }
}
