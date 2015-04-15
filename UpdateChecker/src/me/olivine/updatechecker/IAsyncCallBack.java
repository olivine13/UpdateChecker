package me.olivine.updatechecker;

public abstract class IAsyncCallBack {

	/**
	 * 后台线程，处理耗时操作
	 */
	public abstract boolean workToDo();

	/**
	 * 处理完毕后操作
	 */
	public abstract void onComplete();
	
	/**
	 * 如果在workToDo中返回失败信息，所需要进行的错误处理
	 */
	public void onError(){};

}
