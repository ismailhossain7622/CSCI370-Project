package edu.cs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/FileUploadServlet")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 10,
        maxFileSize = 1024 * 1024 * 50,
        maxRequestSize = 1024 * 1024 * 100
)
public class FileUploadServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String UPLOAD_DIR = "uploads";
    private static final long MAX_UPLOAD_SIZE = 1024 * 1024 * 5;

    // ⭐ POST = upload file
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        String applicationPath = request.getServletContext().getRealPath("");
        String uploadPath = applicationPath + File.separator + UPLOAD_DIR;

        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) uploadDir.mkdirs();

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        try {

            Connection conn = DBConnectionUtil.getConnection();

            String sql =
              "INSERT INTO uploaded_files (filename, content) VALUES (?, ?)";

            String savedFileName = "";

            for (Part part : request.getParts()) {

                String fileName = getFileName(part);

                if (fileName == null || fileName.equals("")) continue;

                if (part.getSize() > MAX_UPLOAD_SIZE) {
                    out.println("❌ File rejected (greater than 5MB)");
                    return;
                }

                savedFileName = fileName;

                part.write(uploadPath + File.separator + fileName);

                PreparedStatement ps = conn.prepareStatement(sql);

                InputStream is = part.getInputStream();

                ps.setString(1, fileName);
                ps.setBlob(2, is);

                ps.executeUpdate();
                ps.close();
            }

            conn.close();

            out.println("<h3>✅ File uploaded successfully</h3>");
            out.println("<a href='FileUploadServlet?view=" + savedFileName + "'>👉 Click here to view file content</a>");

        } catch (Exception e) {
            e.printStackTrace();
            out.println("❌ Database Error: " + e.getMessage());
        }
    }

    // ⭐ GET = show file content
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        String fileName = request.getParameter("view");

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        if (fileName == null) {
            out.println("No file selected");
            return;
        }

        try {

            Connection conn = DBConnectionUtil.getConnection();

            String sql =
              "SELECT content FROM uploaded_files WHERE filename=? ORDER BY id DESC LIMIT 1";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, fileName);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                InputStream is = rs.getBinaryStream("content");

                String content =
                  new java.util.Scanner(is)
                  .useDelimiter("\\Z").next();

                out.println("<h3>📄 File Content:</h3>");
                out.println("<pre>" + content + "</pre>");

            } else {
                out.println("File not found in database");
            }

            rs.close();
            ps.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
            out.println("Error reading file");
        }
    }

    private String getFileName(Part part) {

        String contentDisp = part.getHeader("content-disposition");

        for (String token : contentDisp.split(";")) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf("=") + 2,
                        token.length() - 1);
            }
        }
        return "";
    }
}