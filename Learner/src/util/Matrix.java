package util;

// From: https://introcs.cs.princeton.edu/java/95linear/Matrix.java.html
/******************************************************************************
 *  Compilation:  javac Matrix.java
 *  Execution:    java Matrix
 *
 *  A bare-bones immutable data type for M-by-N matrices.
 *
 ******************************************************************************/

final public class Matrix {
	private final int M;             // number of rows
	private final int N;             // number of columns
	private final Boolean[][] data;   // M-by-N array

	// create M-by-N matrix of 0's
	public Matrix(int M, int N) {
		this.M = M;
		this.N = N;
		data = new Boolean[M][N];
		for (int i = 0; i < M; i++)
			for (int j = 0; j < N; j++)
				data[i][j] = false;
	}

	// create matrix based on 2d array
	public Matrix(Boolean[][] data) {
		M = data.length;
		N = data[0].length;
		this.data = new Boolean[M][N];
		for (int i = 0; i < M; i++)
			for (int j = 0; j < N; j++)
				this.data[i][j] = data[i][j];
	}

	// copy constructor
	private Matrix(Matrix A) { this(A.data); }

	// create and return a random M-by-N matrix with values between 0 and 1
	public static Matrix random(int M, int N) {
		Matrix A = new Matrix(M, N);
		for (int i = 0; i < M; i++)
			for (int j = 0; j < N; j++)
				A.data[i][j] = Math.random() < 0.5;
		return A;
	}

	public void set(int M, int N, Boolean value) {
		data[M][N] = value;
	}

	public Boolean get(int M, int N) {
		return data[M][N];
	}

	// create and return the N-by-N identity matrix
	public static Matrix identity(int N) {
		Matrix I = new Matrix(N, N);
		for (int i = 0; i < N; i++)
			I.data[i][i] = true;
		return I;
	}

	// swap rows i and j
	private void swap(int i, int j) {
		Boolean[] temp = data[i];
		data[i] = data[j];
		data[j] = temp;
	}

	// create and return the transpose of the invoking matrix
	public Matrix transpose() {
		Matrix A = new Matrix(N, M);
		for (int i = 0; i < M; i++)
			for (int j = 0; j < N; j++)
				A.data[j][i] = this.data[i][j];
		return A;
	}

	// does A = B exactly?
	public boolean eq(Matrix B) {
		Matrix A = this;
		if (B.M != A.M || B.N != A.N) throw new RuntimeException("Illegal matrix dimensions.");
		for (int i = 0; i < M; i++)
			for (int j = 0; j < N; j++)
				if (A.data[i][j] != B.data[i][j]) return false;
		return true;
	}

	// return C = A * B
	public Matrix multiply(Matrix B) {
		Matrix A = this;
		if (A.N != B.M) throw new RuntimeException("Illegal matrix dimensions.");
		Matrix C = new Matrix(A.M, B.N);
		for (int i = 0; i < C.M; i++)
			for (int j = 0; j < C.N; j++)
				for (int k = 0; k < A.N; k++) {
					C.data[i][j] = C.data[i][j] || (A.data[i][k] && B.data[k][j]);
					if (C.data[i][j]) // True or \phi = True
						break;
				}
		return C;
	}


	// print matrix to standard output
	public void show() {
		for (int i = 0; i < M; i++) {
			for (int j = 0; j < N; j++)
				System.out.printf("%b ", data[i][j]);
			System.out.println();
		}
	}
}
