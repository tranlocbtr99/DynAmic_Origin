/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: /home/loctm/android-sdk/build-tools/35.0.0/aidl -p/home/loctm/android-sdk/platforms/android-36/framework.aidl -o/home/loctm/merged-origin/bridge/build/generated/aidl_source_output_dir/release/out -I/home/loctm/merged-origin/bridge/src/main/aidl -I/home/loctm/merged-origin/bridge/src/release/aidl -I/home/loctm/.gradle/caches/8.14.3/transforms/489d213ecb391057cf98488157b955c8/transformed/core-1.13.1/aidl -I/home/loctm/.gradle/caches/8.14.3/transforms/45e5de36ea7b98bef848db24df3a41dd/transformed/versionedparcelable-1.1.1/aidl -d/tmp/aidl17232359617111450949.d /home/loctm/merged-origin/bridge/src/main/aidl/com/aikeybridge/shell/IShellService.aidl
 */
package com.aikeybridge.shell;
public interface IShellService extends android.os.IInterface
{
  /** Default implementation for IShellService. */
  public static class Default implements com.aikeybridge.shell.IShellService
  {
    @Override public void destroy() throws android.os.RemoteException
    {
    }
    @Override public java.lang.String exec(java.lang.String command) throws android.os.RemoteException
    {
      return null;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.aikeybridge.shell.IShellService
  {
    /** Construct the stub at attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.aikeybridge.shell.IShellService interface,
     * generating a proxy if needed.
     */
    public static com.aikeybridge.shell.IShellService asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.aikeybridge.shell.IShellService))) {
        return ((com.aikeybridge.shell.IShellService)iin);
      }
      return new com.aikeybridge.shell.IShellService.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(descriptor);
      }
      if (code == INTERFACE_TRANSACTION) {
        reply.writeString(descriptor);
        return true;
      }
      switch (code)
      {
        case TRANSACTION_destroy:
        {
          this.destroy();
          reply.writeNoException();
          break;
        }
        case TRANSACTION_exec:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _result = this.exec(_arg0);
          reply.writeNoException();
          reply.writeString(_result);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements com.aikeybridge.shell.IShellService
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      @Override public void destroy() throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          boolean _status = mRemote.transact(Stub.TRANSACTION_destroy, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public java.lang.String exec(java.lang.String command) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        java.lang.String _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(command);
          boolean _status = mRemote.transact(Stub.TRANSACTION_exec, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readString();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
    }
    static final int TRANSACTION_destroy = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16777114);
    static final int TRANSACTION_exec = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "com.aikeybridge.shell.IShellService";
  public void destroy() throws android.os.RemoteException;
  public java.lang.String exec(java.lang.String command) throws android.os.RemoteException;
}
