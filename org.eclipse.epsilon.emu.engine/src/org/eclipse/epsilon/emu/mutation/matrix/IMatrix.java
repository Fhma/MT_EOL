package org.eclipse.epsilon.emu.mutation.matrix;

import java.io.IOException;
import java.io.Serializable;

public interface IMatrix extends Serializable {
	public boolean saveMatrix() throws IOException;

	public boolean loadMatrix() throws ClassNotFoundException, IOException;

	public String getFileExtension();

	public void setPath(String path);

	public String getPath();
}
