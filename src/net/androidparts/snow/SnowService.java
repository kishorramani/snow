package net.androidparts.snow;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class SnowService extends WallpaperService {

    private final static int[] windCurve = {1,3,5,6,6,6,7,7,6,5,4,2,1,1};

    public Engine onCreateEngine() {
        return new SnowEngine();
    }

    public class SnowEngine extends WallpaperService.Engine {

        private SnowThread thread;
        private Random random = new Random();
        private SurfaceHolder holder;

        private Bitmap flakeImage;
        private Set<Snowflake> flakes = new HashSet<Snowflake>();
        private Set<Snowflake> oobFlakes = new HashSet<Snowflake>();
        private int wind = 0;

        public SnowEngine() {
            flakeImage = BitmapFactory.decodeResource(getResources(), R.drawable.square3x3);
        }

        public class SnowThread extends Thread {

            private boolean isSnowing = true;
            private long lastWindUpdate = System.currentTimeMillis();
            private int windCurveIndex = 0;

            private void update(Canvas c) {

                c.drawRGB(0, 0, 0);
                flakes.add(new Snowflake(random.nextFloat() * c.getWidth(), -flakeImage.getWidth(), flakeImage));
                long nowTime = System.currentTimeMillis();

                if (wind == 0) {
                   if (nowTime - lastWindUpdate > 20000) {
                      wind = windCurve[++windCurveIndex];
                      lastWindUpdate = nowTime;
                   }
                } else if (nowTime - lastWindUpdate > 1000) {
                    if (windCurveIndex < windCurve.length -1) {
                        wind = windCurve[++windCurveIndex];
                        lastWindUpdate = nowTime;
                    } else {
                        windCurveIndex = 0;
                        wind = 0;
                        lastWindUpdate = nowTime;
                    }
                }


                for (Snowflake flake : flakes) {
                    flake.update(wind);
                    c.drawBitmap(flake.bitmap, flake.x, flake.y, null);

                    if (flake.x > c.getWidth() || flake.x < -flake.bitmap.getWidth() ||
                            flake.y > c.getHeight()) {
                        oobFlakes.add(flake);
                    }

                }

                flakes.removeAll(oobFlakes);
                oobFlakes.clear();

            }

            @Override
            public void run() {
                Log.d("snow", "SnowThread started");
                while (isSnowing) {
                    Canvas canvas = null;
                    try {
                        Thread.sleep(30);
                        canvas = holder.lockCanvas();
                        update(canvas);

                    } catch (InterruptedException e) {

                    } finally {
                        if (canvas != null) {
                            holder.unlockCanvasAndPost(canvas);
                        }
                    }
                }
            }

            public boolean isSnowing() {
                return isSnowing;
            }

            public void stopSnowing() {
                isSnowing = false;
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            Log.d("snow", "visibilityChanged to " + visible);
            if (visible) {
                thread = new SnowThread();
                thread.start();
            } else {
                thread.stopSnowing();
            }
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            Log.d("snow", "surfaceCreated");
            this.holder = holder;
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            // usual case is onVisibilityChanged(false) has been called
            // and  the thread is shutting down
            Log.d("snow", "surfaceDestroyed");
            boolean retry = true;
            while (retry) {
                try {
                    thread.stopSnowing(); // just in case
                    thread.join();
                    Log.d("snow", "snowThread died");
                    retry = false;
                } catch (InterruptedException e) {
                }
            }
        }


        class Snowflake {

            public Bitmap bitmap;
            float x;
            float y;
            int bias = 0;
            int biasTime = 0;
            boolean interBias = false;

            Snowflake(float x, float y, Bitmap bitmap) {
                this.x = x;
                this.y = y;
                this.bitmap = bitmap;
            }

            void update(int wind) {

                if (biasTime < 0) {
                    if (interBias) {
                        bias = ((Integer) random.nextInt(3)).compareTo(1);
                        interBias = false;
                    } else {
                        bias = 0;
                        interBias = true;
                    }

                    biasTime = random.nextInt(30);
                }

                biasTime--;
                x += bias + wind;
                y += 4;
            }
        }
    }
}
