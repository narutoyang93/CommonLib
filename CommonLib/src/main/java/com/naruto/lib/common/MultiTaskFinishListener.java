package com.naruto.lib.common;

import java.util.HashSet;
import java.util.Set;

/**
 * @Description 监听多个异步任务全部完成
 * @Author Naruto Yang
 * @CreateDate 2021/5/20 0020
 * @Note
 */
public abstract class MultiTaskFinishListener {
    private Set<String> taskTagSet = new HashSet<>();

    public synchronized void reset() {
        taskTagSet.clear();
    }

    public synchronized void start(String taskTag) {
        taskTagSet.add(taskTag);
    }

    public synchronized void finish(String taskTag) {
        taskTagSet.remove(taskTag);
        if (isAllTaskFinish()) onAllTasksFinished();
    }

    public boolean isAllTaskFinish(){
        return taskTagSet.isEmpty();
    }

    public synchronized boolean isTaskExist(String taskTag) {
        return taskTagSet.contains(taskTag);
    }

    protected abstract void onAllTasksFinished();
}
