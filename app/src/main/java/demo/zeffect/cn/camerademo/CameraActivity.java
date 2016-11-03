package demo.zeffect.cn.camerademo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 拍照界面
 * Created by zeffect on 2016/10/28.
 *
 * @auther zzx
 */
public class CameraActivity extends Activity {
    /**
     * 预览界面
     */
    private SurfaceView mCameraSurface;
    /**
     * Surface持有者
     */
    private SurfaceHolder mSurfaceHolder;
    /***
     * 标记是否在预览中
     */
    private boolean isPreview = false;
    /**
     * Camera
     */
    private Camera mCamera;
    /**
     * 拍照按钮,切换摄像头按钮
     */
    private Button takePhoto, switchBtn;
    /**
     * 默认的摄像头位置
     */
    private int mDefaultCameraPosition = 0;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_layout);
        initView();
        initEnvent();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mCamera != null) {
            mCamera.release();
        }
        super.onDestroy();
    }

    /**
     * 初始化控件
     */
    private void initView() {
        takePhoto = (Button) findViewById(R.id.cl_take_btn);
        switchBtn = (Button) findViewById(R.id.cl_switch_btn);
        mCameraSurface = (SurfaceView) findViewById(R.id.cl_surface);
    }

    /**
     * 初始化一些事件
     */
    private void initEnvent() {
        mSurfaceHolder = mCameraSurface.getHolder();
        mSurfaceHolder.addCallback(mySurfaceCallback);
        switchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View pView) {
                swicthCamera();
            }
        });
        mCameraSurface.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View pView, MotionEvent pEvent) {
                Point nowPoint = new Point((int) pEvent.getX(), (int) pEvent.getY());
                onFocus(nowPoint, mAutoFocusCallback);
                return true;
            }
        });
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View pView) {
                mCamera.takePicture(null, null, mPictureCallback);
            }
        });
    }

    /**
     * 初始化相机
     *
     * @param holder
     * @throws IOException
     */
    @SuppressWarnings("deprecation")
    protected void initCamera(SurfaceHolder holder) throws IOException {
        if (!isPreview) {
            mCamera = Camera.open(mDefaultCameraPosition);//1前置0后置
        }
        mCamera.setDisplayOrientation(0);//设置摄像头方向(如本来是横屏，显示的是竖屏)
        if (mCamera != null && !isPreview) {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
            mCamera.autoFocus(mAutoFocusCallback);
            isPreview = true;
            initCamera();
        }
    }

    // 初始化相机
    public void initCamera() {
        if (null != mCamera) {
            Camera.Parameters myParam = mCamera.getParameters();
            int picbestWidth = 0;
            int picbestHeight = 0;
            int prebestWidth = 0;
            int prebestHeight = 0;
            List<Camera.Size> pictureSizes = myParam.getSupportedPictureSizes();
            List<Camera.Size> previewSizes = myParam.getSupportedPreviewSizes();
            for (int i = 0; i < previewSizes.size(); i++) {
                Camera.Size size = previewSizes.get(i);
                if (size.width > prebestWidth && size.height > prebestHeight) {
                    prebestWidth = size.width;
                    prebestHeight = size.height;
                }
            }
            for (int i = 0; i < pictureSizes.size(); i++) {
                Camera.Size size = pictureSizes.get(i);
                if (size.width > picbestWidth && size.height > picbestHeight && size.width <= prebestWidth && size.height <= prebestHeight) {
                    picbestWidth = size.width;
                    picbestHeight = size.height;
                }
            }
            myParam.setPictureSize(picbestWidth, picbestHeight);// 照片实际尺寸。
            mCamera.setParameters(myParam);// ToneGenerator
            mCamera.startPreview();// 开始预览。
        }
    }

    /**
     * surface回调，用来接收创建，改变，消失事件
     */
    SurfaceHolder.Callback mySurfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder pHolder) {
            try {
                initCamera(pHolder);
            } catch (IOException pE) {
                pE.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder pHolder, int pI, int pI1, int pI2) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder pHolder) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            mSurfaceHolder = null;
            mCameraSurface = null;
        }
    };
    /**
     * 完成自动聚焦后的回调方法**
     */
    Camera.AutoFocusCallback mAutoFocusCallback = new Camera.AutoFocusCallback() {

        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (success) {
                mCamera.setOneShotPreviewCallback(null);
            }
        }
    };
    /**
     * 拍照完成后回调
     */
    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            //根据拍照的数据创建位图
            final Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
            File file = new File(CameraActivity.this.getExternalFilesDir("photo"), System.currentTimeMillis() + ".jpg");
            FileOutputStream outStream = null;
            try {
                outStream = new FileOutputStream(file);
                bm.compress(Bitmap.CompressFormat.JPEG, 80, outStream);
                outStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            camera.stopPreview();
            camera.startPreview();
            isPreview = true;
        }
    };

    private void swicthCamera() {
        //切换前后摄像头
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();//得到摄像头的个数

        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);//得到每一个摄像头的信息
            if (mDefaultCameraPosition == 1) {
                //现在是后置，变更为前置
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                    mCamera.stopPreview();//停掉原来摄像头的预览
                    mCamera.release();//释放资源
                    mCamera = null;//取消原来摄像头
                    mCamera = Camera.open(i);//打开当前选中的摄像头
                    try {
                        mCamera.setPreviewDisplay(mSurfaceHolder);//通过surfaceview显示取景画面
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    mCamera.startPreview();//开始预览
                    mDefaultCameraPosition = 0;
                    break;
                }
            } else {
                //现在是前置， 变更为后置
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                    mCamera.stopPreview();//停掉原来摄像头的预览
                    mCamera.release();//释放资源
                    mCamera = null;//取消原来摄像头
                    mCamera = Camera.open(i);//打开当前选中的摄像头
                    try {
                        mCamera.setPreviewDisplay(mSurfaceHolder);//通过surfaceview显示取景画面
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    mCamera.startPreview();//开始预览
                    mDefaultCameraPosition = 1;
                    break;
                }
            }
        }
    }

    /**
     * 手动聚焦
     *
     * @param point 触屏坐标
     */
    protected boolean onFocus(Point point, Camera.AutoFocusCallback callback) {
        if (mCamera == null) {
            return false;
        }

        Camera.Parameters parameters = null;
        try {
            parameters = mCamera.getParameters();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        //不支持设置自定义聚焦，则使用自动聚焦，返回
        if (Build.VERSION.SDK_INT >= 14) {
            if (parameters.getMaxNumFocusAreas() <= 0) {
                return focus(callback);
            }
            //定点对焦
            List<Camera.Area> areas = new ArrayList<Camera.Area>();
            int left = point.x - 300;
            int top = point.y - 300;
            int right = point.x + 300;
            int bottom = point.y + 300;
            left = left < -1000 ? -1000 : left;
            top = top < -1000 ? -1000 : top;
            right = right > 1000 ? 1000 : right;
            bottom = bottom > 1000 ? 1000 : bottom;
            areas.add(new Camera.Area(new Rect(left, top, right, bottom), 100));
            parameters.setFocusAreas(areas);
            try {
                //本人使用的小米手机在设置聚焦区域的时候经常会出异常，看日志发现是框架层的字符串转int的时候出错了，
                //目测是小米修改了框架层代码导致，在此try掉，对实际聚焦效果没影响
                mCamera.setParameters(parameters);
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
                return false;
            }
        }
        return focus(callback);
    }

    private boolean focus(Camera.AutoFocusCallback callback) {
        try {
            mCamera.autoFocus(callback);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
