package org.karmaexchange.dao;

public enum Permission {
  READ {
    @Override
    public boolean canEdit() {
      return false;
    }
  },
  EDIT {
    @Override
    public boolean canEdit() {
      return true;
    }
  },
  ALL {
    @Override
    public boolean canEdit() {
      return true;
    }
  };

  public abstract boolean canEdit();
}
