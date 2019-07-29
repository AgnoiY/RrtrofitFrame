package com.cjy.retrofitlibrary;

import android.content.Context;
import android.text.TextUtils;

import com.cjy.retrofitlibrary.annotation.loadingdialog.DialogClose;
import com.cjy.retrofitlibrary.annotation.loadingdialog.DialogShow;
import com.cjy.retrofitlibrary.dialog.LoadingDialog;
import com.cjy.retrofitlibrary.utils.AnnotationUtils;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * 适用Retrofit网络请求Observer(监听者)
 * 备注:
 * 1.重写onSubscribe，添加请求标识
 * 2.重写onError，移除请求
 * 3.重写cancel，取消请求
 * 4.重写onNext，成功请求
 * 5.ProgressDialogObserver,加载的提示窗
 * Data：2018/12/18
 *
 * @author yong
 */
abstract class BaseObserver<T> implements Observer<T>, ProgressDialogObserver, RequestCancel {

    /**
     * 请求标识
     */
    private String mTag;
    private Disposable d;
    private Object loadingDialog;

    public BaseObserver() {
    }

    public BaseObserver(Context context, boolean isDialog, boolean isCabcelble) {
        if (isDialog) {
            loadingDialog = AnnotationUtils.newConstructor(getDialogClass(), context, isCabcelble, this);
        }
    }

    @Override
    public void onSubscribe(Disposable d) {
        this.d = d;
        showProgressDialog();
        if (!TextUtils.isEmpty(mTag)) {
            RequestManagerImpl.getInstance().add(mTag, d);
        }
    }

    @Override
    public void onNext(T t) {
        if (!TextUtils.isEmpty(mTag)) {
            RequestManagerImpl.getInstance().remove(mTag);
        }
    }

    @Override
    public void onError(Throwable e) {

        hideProgressDialog();

        if (!TextUtils.isEmpty(mTag)) {
            RequestManagerImpl.getInstance().remove(mTag);
        }
    }

    @Override
    public void onComplete() {
        hideProgressDialog();
        /**
         * 由于LifecycleProvider取消监听直接截断事件发送，但是必定回调onComplete()
         * 因此在这里判断请求是否被取消，如果到这里还未被取消，说明是LifecycleProvider导致的取消请求，回调onCancel逻辑
         * 备注：
         * 1.子类重写此方法时需要调用super
         * 2.多个请求复用一个监听者HttpObserver时，tag会被覆盖，取消回调会有误
         */
        if (!RequestManagerImpl.getInstance().isDisposed(mTag)) {
            cancel();
        }
    }

    /**
     * 显示Dialog提示框
     */
    @Override
    public void showProgressDialog() {
        if (null != loadingDialog) {
            AnnotationUtils.getMethod(loadingDialog, getDialogClass(), DialogShow.class);
        }
    }

    /**
     * 隐藏Dialog提示框
     */
    @Override
    public void hideProgressDialog() {
        if (null != loadingDialog) {
            AnnotationUtils.getMethod(loadingDialog, getDialogClass(), DialogClose.class);
            loadingDialog = null;
        }
    }

    @Override
    public void onCancleProgress() {
        if (!d.isDisposed())
            d.dispose();
    }

    /**
     * 手动取消请求
     */
    @Override
    public void cancel() {
        if (!TextUtils.isEmpty(mTag)) {
            RequestManagerImpl.getInstance().cancel(mTag);
            hideProgressDialog();
        }
    }

    /**
     * 设置标识请求的TAG
     *
     * @param tag
     */
    public void setTag(String tag) {
        this.mTag = tag;
    }

    /**
     * 获取请求标识
     *
     * @return
     */
    public String getTag() {
        return mTag;
    }

    /**
     * 获取加载弹窗
     *
     * @return
     */
    private Class getDialogClass() {
        Class var = RetrofitHttp.Configure.get().getDialogClass();
        if (var == null) {
            var = LoadingDialog.class;
        }
        return var;
    }
}
