package me.olivine.updatechecker;

public class VersionInfo {

	/**
	 * �汾��
	 */
	int mVersionCode;

	/**
	 * �汾��
	 */
	String mVersionName;

	/**
	 * �汾���ص�ַ
	 */
	String mDownloadUrl;

	/**
	 * ������Ϣ
	 */
	String mUpdateInfo;

	public int getVersionCode() {
		return this.mVersionCode;
	}

	public String getVersionName() {
		return this.mVersionName;
	}

	public String getDownloadUrl() {
		return this.mDownloadUrl;
	}

	public String getUpdateInfo() {
		return this.mUpdateInfo;
	}

	public void setVersionCode(int pVersionCode) {
		this.mVersionCode = pVersionCode;
	}

	public void setVersionName(String pVersionName) {
		this.mVersionName = pVersionName;
	}

	public void setDownloadUrl(String pDownloadUrl) {
		this.mDownloadUrl = pDownloadUrl;
	}

	public void setUpdateInfo(String pUpdateInfo) {
		this.mUpdateInfo = pUpdateInfo;
	}
}
