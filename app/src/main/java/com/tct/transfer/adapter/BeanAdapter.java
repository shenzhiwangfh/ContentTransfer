package com.tct.transfer.adapter;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tct.transfer.R;
import com.tct.transfer.database.FileBeanHelper;
import com.tct.transfer.file.FileBean;
import com.tct.transfer.log.LogUtils;
import com.tct.transfer.util.DefaultValue;
import com.tct.transfer.util.FileSizeUtil;
import com.tct.transfer.util.Utils;

import java.io.File;
import java.util.ArrayList;

public class BeanAdapter extends RecyclerView.Adapter<BeanAdapter.ViewHolder> implements View.OnClickListener, View.OnLongClickListener {

    private final static String TAG = "BeanAdapter";

    private Context mContext;
    private ArrayList<FileBean> mBeans = new ArrayList<>();
    private int[] mTypes;

    private OnItemClickListener mItemClickListener;
    private OnItemLongClickListener mItemLongClickListener;

    public interface OnItemClickListener {
        void onItemClick(FileBean bean);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(FileBean bean);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mItemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        mItemLongClickListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private View v;
        public ImageView viewType;
        public TextView viewName;
        public TextView viewTime;
        public TextView viewSize;
        public ImageView viewAction;
        public ImageView viewResult;

        // TODO Auto-generated method stub
        public ViewHolder(View v) {
            super(v);
            this.v = v;
        }
    }

    public BeanAdapter(Context context, ArrayList<FileBean> beans) {
        mContext = context;
        //mBeans = beans;

        TypedArray array = mContext.getResources().obtainTypedArray(R.array.file_types);
        mTypes = new int[array.length()];
        for (int i = 0; i < array.length(); i++) {
            mTypes[i] = array.getResourceId(i, 0);
        }

        changeBeans();
    }

    //public void setBeans(ArrayList<FileBean> beans) {
    //    mBeans = beans;
    //}

    public void changeBeans() {
        mBeans.clear();

        final String sortOrder = FileBeanHelper._ID + " desc";
        Cursor cursor = mContext.getContentResolver().query(
                DefaultValue.uri, null, null, null, sortOrder);
        if (cursor != null && cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                FileBean bean = new FileBean();
                bean._id = cursor.getInt(cursor.getColumnIndex(FileBeanHelper._ID));
                bean.path = cursor.getString(cursor.getColumnIndex(FileBeanHelper.PATH));
                bean.name = cursor.getString(cursor.getColumnIndex(FileBeanHelper.NAME));
                bean.md5 = cursor.getString(cursor.getColumnIndex(FileBeanHelper.MD5));
                bean.size = cursor.getLong(cursor.getColumnIndex(FileBeanHelper.SIZE));
                bean.transferSize = cursor.getLong(cursor.getColumnIndex(FileBeanHelper.TRANSFER_SIZE));
                bean.action = cursor.getInt(cursor.getColumnIndex(FileBeanHelper.TRANSFER_ACTION));
                bean.time = cursor.getLong(cursor.getColumnIndex(FileBeanHelper.TIME));
                bean.elapsed = cursor.getLong(cursor.getColumnIndex(FileBeanHelper.ELAPSED));
                bean.type = cursor.getInt(cursor.getColumnIndex(FileBeanHelper.TYPE));
                bean.status = cursor.getInt(cursor.getColumnIndex(FileBeanHelper.STATUS));
                bean.result = cursor.getInt(cursor.getColumnIndex(FileBeanHelper.RESULT));
                bean.uri = cursor.getString(cursor.getColumnIndex(FileBeanHelper.URI));
                mBeans.add(bean);
                cursor.moveToNext();
            }
            cursor.close();
        }

        notifyDataSetChanged();
    }

    @Override
    public BeanAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.file_bean, viewGroup, false);
        BeanAdapter.ViewHolder holder = new BeanAdapter.ViewHolder(v);
        holder.viewType = v.findViewById(R.id.bean_type);
        holder.viewName = v.findViewById(R.id.bean_name);
        holder.viewTime = v.findViewById(R.id.bean_time);
        holder.viewSize = v.findViewById(R.id.bean_size);
        holder.viewAction = v.findViewById(R.id.bean_action);
        holder.viewResult = v.findViewById(R.id.bean_result);

        v.setOnClickListener(this);
        v.setOnLongClickListener(this);
        return holder;
    }

    @Override
    public void onBindViewHolder(BeanAdapter.ViewHolder viewHolder, final int position) {
        // TODO Auto-generated method stub
        FileBean bean = mBeans.get(position);

        viewHolder.viewType.setImageResource(mTypes[bean.type]);
        viewHolder.viewName.setText(bean.name);
        viewHolder.viewTime.setText(Utils.long2time(bean.time));
        viewHolder.viewSize.setText(FileSizeUtil.FormetFileSize(bean.size));
        viewHolder.viewAction.setImageResource(bean.action == 0 ? R.drawable.ic_upload : R.drawable.ic_download);
        viewHolder.viewResult.setImageResource(bean.result == 0 ? R.drawable.result_ok : R.drawable.result_error);

        viewHolder.v.setTag(bean);
    }

    @Override
    public int getItemCount() {
        // TODO Auto-generated method stub
        return (mBeans == null || mBeans.size() == 0) ? 0 : mBeans.size();
    }

    @Override
    public void onClick(View v) {
        if (mItemClickListener != null) {
            mItemClickListener.onItemClick((FileBean) v.getTag());
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (mItemLongClickListener != null) {
            mItemLongClickListener.onItemLongClick((FileBean) v.getTag());
            return true;
        }
        return false;
    }
}
