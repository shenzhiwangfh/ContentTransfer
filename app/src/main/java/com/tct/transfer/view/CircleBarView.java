package com.tct.transfer.view;

import android.Manifest;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.TextView;

import com.tct.transfer.R;
import com.tct.transfer.file.FileBean;
import com.tct.transfer.log.LogUtils;

/**
 * Created by anlia on 2017/10/10.
 */

public class CircleBarView extends View {

    private final static double PI = 3.1415;

    private Paint bgPaint;//绘制背景圆弧的画笔
    private Paint progressPaint;//绘制圆弧的画笔
    private Paint textPaint;

    private RectF mRectF;//绘制圆弧的矩形区域

    private CircleBarAnim anim;

    private float progressNum;//可以更新的进度条数值
    private float maxNum;//进度条最大值

    private int progressUpColor;//进度条圆弧颜色
    private int progressDownColor;//进度条圆弧颜色
    private int progressErrColor;
    private int bgColor;//背景圆弧颜色
    private float startAngle;//背景圆弧的起始角度
    private float sweepAngle;//背景圆弧扫过的角度
    private float barWidth;//圆弧进度条宽度
    private float padding;

    private int defaultSize;//自定义View默认的宽高
    private float progressSweepAngle;//进度条圆弧扫过的角度

    private TextView textView;
    private OnAnimationListener onAnimationListener;

    private String text;
    private FileBean bean;

    public CircleBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleBarView);

        progressUpColor = typedArray.getColor(R.styleable.CircleBarView_progress_up_color, Color.YELLOW);
        progressDownColor = typedArray.getColor(R.styleable.CircleBarView_progress_down_color, Color.GREEN);
        progressErrColor = typedArray.getColor(R.styleable.CircleBarView_progress_error_color, Color.RED);
        bgColor = typedArray.getColor(R.styleable.CircleBarView_bg_color, Color.GRAY);
        startAngle = typedArray.getFloat(R.styleable.CircleBarView_start_angle, 0);
        sweepAngle = typedArray.getFloat(R.styleable.CircleBarView_sweep_angle, 360);
        barWidth = typedArray.getDimension(R.styleable.CircleBarView_bar_width, DpOrPxUtils.dip2px(context, 10));
        padding = typedArray.getDimension(R.styleable.CircleBarView_padding, 0);
        typedArray.recycle();

        progressNum = 0;
        maxNum = 100;
        defaultSize = DpOrPxUtils.dip2px(context, 1000);
        mRectF = new RectF();

        progressPaint = new Paint();
        progressPaint.setStyle(Paint.Style.STROKE);//只描边，不填充
        //progressPaint.setColor(progressColor);
        progressPaint.setAntiAlias(true);//设置抗锯齿
        progressPaint.setStrokeWidth(barWidth);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);//设置画笔为圆角

        bgPaint = new Paint();
        bgPaint.setStyle(Paint.Style.STROKE);//只描边，不填充
        bgPaint.setColor(bgColor);
        bgPaint.setAntiAlias(true);//设置抗锯齿
        bgPaint.setStrokeWidth(barWidth);
        bgPaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint = new Paint();
        textPaint.setColor(Color.GRAY);
        textPaint.setTextSize(48);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        Typeface typeface = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            typeface = getResources().getFont(R.font.myfont);
        }
        if(typeface != null) textPaint.setTypeface(typeface);

        anim = new CircleBarAnim();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int height = measureSize(defaultSize, heightMeasureSpec);
        int width = measureSize(defaultSize, widthMeasureSpec);
        int min = Math.min(width, height);// 获取View最短边的长度
        setMeasuredDimension(min, min);// 强制改View为以最短边为长度的正方形

        if (min >= barWidth * 2) {
            float left = (barWidth / 2) + padding;
            float top = (barWidth / 2) + padding;
            float right = min - (barWidth / 2) - padding;
            float bottom = min - (barWidth / 2) - padding;
            mRectF.set(left, top, right, bottom);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawArc(mRectF, startAngle, sweepAngle, false, bgPaint);
        if(bean != null) {
            if (bean.action == 0) { //upload
                progressPaint.setColor(bean.result == 0 ? progressUpColor : progressErrColor);
                canvas.drawArc(mRectF, startAngle, progressSweepAngle, false, progressPaint);
            } else if (bean.action == 1) { //download
                progressPaint.setColor(bean.result == 0 ? progressDownColor : progressErrColor);
                canvas.drawArc(mRectF, startAngle + sweepAngle - progressSweepAngle, progressSweepAngle, false, progressPaint);
            }
        }

        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float top = fontMetrics.top;//为基线到字体上边框的距离,即上图中的top
        float bottom = fontMetrics.bottom;//为基线到字体下边框的距离,即上图中的bottom
        int baseLineY = (int) (mRectF.centerY() - top / 2 - bottom / 2) / 3;//基线中间点的y轴计算公式
        if(text != null && !text.isEmpty())
            canvas.drawText(text, mRectF.centerX(), baseLineY, textPaint);
    }

    public class CircleBarAnim extends Animation {

        public CircleBarAnim() {
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {//interpolatedTime从0渐变成1,到1时结束动画,持续时间由setDuration（time）方法设置
            super.applyTransformation(interpolatedTime, t);
            progressSweepAngle = interpolatedTime * sweepAngle * progressNum / maxNum;
            if (onAnimationListener != null) {
                if (textView != null) {
                    textView.setText(onAnimationListener.howToChangeText(interpolatedTime, progressNum, maxNum));
                }
                onAnimationListener.howToChangeProgressColor(progressPaint, interpolatedTime, progressNum, maxNum);
            }
            postInvalidate();
        }
    }

    private int measureSize(int defaultSize, int measureSpec) {
        int result = defaultSize;
        int specMode = View.MeasureSpec.getMode(measureSpec);
        int specSize = View.MeasureSpec.getSize(measureSpec);

        if (specMode == View.MeasureSpec.EXACTLY) {
            result = specSize;
        } else if (specMode == View.MeasureSpec.AT_MOST) {
            result = Math.min(result, specSize);
        }
        return result;
    }


    /**
     * 设置进度条最大值
     *
     * @param maxNum
     */
    public void setMaxNum(float maxNum) {
        this.maxNum = maxNum;
    }

    /**
     * 设置进度条数值
     *
     * @param progressNum 进度条数值
     * @param time        动画持续时间
     */
    public void setProgressNum(float progressNum, int time, String text, FileBean bean) {
        this.progressNum = progressNum;
        this.text = text;
        this.bean = bean;
        progressSweepAngle = sweepAngle * progressNum * 100 / maxNum;

        invalidate();
        //anim.setDuration(time);
        //this.startAnimation(anim);
    }

    public void reset() {
        this.progressNum = 0;
        this.text = null;
        this.bean = null;
        progressSweepAngle = sweepAngle * progressNum * 100 / maxNum;

        invalidate();
    }

    /**
     * 设置显示文字的TextView
     *
     * @param textView
     */
    public void setTextView(TextView textView) {
        this.textView = textView;
    }

    public interface OnAnimationListener {

        /**
         * 如何处理要显示的文字内容
         *
         * @param interpolatedTime 从0渐变成1,到1时结束动画
         * @param updateNum        进度条数值
         * @param maxNum           进度条最大值
         * @return
         */
        String howToChangeText(float interpolatedTime, float updateNum, float maxNum);

        /**
         * 如何处理进度条的颜色
         *
         * @param paint            进度条画笔
         * @param interpolatedTime 从0渐变成1,到1时结束动画
         * @param updateNum        进度条数值
         * @param maxNum           进度条最大值
         */
        void howToChangeProgressColor(Paint paint, float interpolatedTime, float updateNum, float maxNum);

    }

    public void setOnAnimationListener(OnAnimationListener onAnimationListener) {
        this.onAnimationListener = onAnimationListener;
    }
}
