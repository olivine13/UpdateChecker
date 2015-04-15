package me.olivine.updatechecker;

public class VersionInfo {

	/**
	 * 版本号
	 */
	int mVersionCode;

	/**
	 * 版本名
	 */
	String mVersionName;

	/**
	 * 版本下载地址
	 */
	String mDownloadUrl;

	/**
	 * 更新信息
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
