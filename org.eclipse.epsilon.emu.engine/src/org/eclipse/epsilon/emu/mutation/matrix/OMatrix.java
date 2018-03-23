package org.eclipse.epsilon.emu.mutation.matrix;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OMatrix implements IMatrix {

	private static final long serialVersionUID = 1L;
	private String path;
	private HashMap<String, List<String>> content;

	public OMatrix(String path) {
		super();
		this.path = path;
	}

	@SuppressWarnings("unused")
	private OMatrix() {
	}

	@Override
	public boolean saveMatrix() throws IOException {
		try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(path + getFileExtension()))) {
			os.writeObject(getContent());
			return true;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean loadMatrix() throws IOException, ClassNotFoundException {
		try (ObjectInputStream is = new ObjectInputStream(new FileInputStream(path + getFileExtension()))) {
			content = (HashMap<String, List<String>>) is.readObject();
			return true;
		}
	}

	@Override
	public String getFileExtension() {
		return ".omtr";
	}

	@Override
	public void setPath(String path) {
		this.path = path;

	}

	@Override
	public String getPath() {
		return path;
	}

	public void setContent(HashMap<String, List<String>> content) {
		this.content = content;
	}

	public HashMap<String, List<String>> getContent() {
		if (content == null)
			content = new HashMap<String, List<String>>();
		return content;
	}

	public List<String> getValue(String key) {
		if (getContent().get(key) == null)
			getContent().put(key, new ArrayList<String>());
		return getContent().get(key);
	}
}
