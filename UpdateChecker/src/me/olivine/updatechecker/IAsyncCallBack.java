package me.olivine.updatechecker;

public abstract class IAsyncCallBack {

	/**
	 * ��̨�̣߳������ʱ����
	 */
	public abstract boolean workToDo();

	/**
	 * ������Ϻ����
	 */
	public abstract void onComplete();
	
	/**
	 * �����workToDo�з���ʧ����Ϣ������Ҫ���еĴ�����
	 */
	public void onError(){};

}
