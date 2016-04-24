package haven;


import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

public class TimersThread extends Thread {
    private List<TimerWdg> timers = new ArrayList<TimerWdg>();
    private long time, epoch;

    public void tick(long time, long epoch) {
        this.time = time;
        this.epoch = epoch;
    }

    @Override
    public void run() {
        while (true) {
            synchronized (timers) {
                for (int i = 0; i < timers.size(); i++) {
                    TimerWdg timer = timers.get(i);
                    if (!timer.active)
                        continue;

                    long srvtime = globtime();
                    if (srvtime != 0) {
                        timer.elapsed = Math.round(srvtime / 3.0) - timer.start;
                        timer.updateRemaining();

                        if (timer.elapsed >= timer.duration) {
                            timer.done();
                            i--;
                        }
                    }
                }
            }
            try {
                sleep(1000);
            } catch (InterruptedException e) {
            }
        }
    }

    public TimerWdg add(String name, long duration, long start) {
        synchronized (timers) {
            TimerWdg timer = new TimerWdg(name, duration, start);
            timers.add(timer);
            return timer;
        }
    }

    public void remove(TimerWdg timer) {
        synchronized (timers) {
            timers.remove(timer);
        }
    }

    public List<TimerWdg> getall() {
        synchronized (timers) {
            return timers;
        }
    }

    public void save() {
        synchronized (timers) {
            JSONObject[] timersjson = new JSONObject[timers.size()];
            for (int i = 0; i < timers.size(); i++) {
                final TimerWdg timer = timers.get(i);
                timersjson[i] = new JSONObject()
                        .put("name", timer.name)
                        .put("duration", timer.duration)
                        .put("start", timer.active ? timer.start : 0);
            }
            Utils.setprefjsona("timers", timersjson);
        }
    }

    public void load() {
        JSONObject[] tstarr = Utils.getprefjsona("timers", null);
        if (tstarr == null)
            return;
        for (int i = 0; i < tstarr.length; i++) {
            JSONObject t = tstarr[i];
            long start = 0;
            try {
                start = t.getLong("start");
            } catch (JSONException e) {
            }
            add(t.getString("name"), t.getLong("duration"), start);
        }
    }

    private long lastrep = 0;
    private long rgtime = 0;

    public long globtime() {
        if (time == 0 || epoch == 0)
            return 0;

        long now = System.currentTimeMillis();
        long raw = ((now - epoch) * 3) + (time * 1000);
        if (lastrep == 0) {
            rgtime = raw;
        } else {
            long gd = (now - lastrep) * 3;
            rgtime += gd;
        }
        lastrep = now;
        return rgtime;
    }
}

