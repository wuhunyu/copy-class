package top.wuhunyu.copyclass;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * class 复制插件
 *
 * @author gongzhiqiang
 * @date 2024-11-01 15:44
 */

public class CopyClassAction extends AnAction {

    private static final PluginDescriptor COPY_CLASS =
            PluginManagerCore.getPlugin(PluginId.getId("top.wuhunyu.plugin.copy-class"));

    private static final String PROJECT_TYPE_ERROR = "Unable to determine the type of current project";

    private static final String GET_JAVA_FILE_ERROR = "A single .java file needs to be selected";

    private static final String COMPILE_PROJECT_ERROR = "Please compile the project first";

    private static final String COPY_SUCCESSFULLY = "Copy successfully";

    private static final String SOURCE_SUFFIX = ".java";

    private static final String TARGET_SUFFIX = ".class";

    private static final Map<String, List<String>> CLASS_PATHS = Map.of(
            "src/main/java",
            List.of("target/classes", "build/classes/java/main"),
            "src/test/java",
            List.of("target/test-classes", "build/classes/java/test")
    );

    @Override
    public void actionPerformed(@NotNull AnActionEvent actionEvent) {
        final Project project = actionEvent.getProject();
        // Get the currently selected file
        final VirtualFile virtualFile = actionEvent.getData(CommonDataKeys.VIRTUAL_FILE);
        // Determine whether the current file is a java file
        if (Objects.isNull(virtualFile) ||
                !virtualFile.isValid() ||
                virtualFile.isDirectory() ||
                !virtualFile.getName().endsWith(SOURCE_SUFFIX)) {
            this.fastNotice(project, GET_JAVA_FILE_ERROR);
            return;
        }
        String curPath = virtualFile.getPath();
        boolean existsJavaPath = Boolean.FALSE;
        boolean existsClassPath = Boolean.FALSE;
        String targetPath = null;
        outer:
        for (Map.Entry<String, List<String>> entry : CLASS_PATHS.entrySet()) {
            String javaPath = entry.getKey();
            List<String> classPaths = entry.getValue();
            if (!curPath.contains(javaPath)) {
                continue;
            }
            existsJavaPath = Boolean.TRUE;
            for (String classPath : classPaths) {
                targetPath = curPath.replace(javaPath, classPath)
                        .replace(SOURCE_SUFFIX, TARGET_SUFFIX);
                if (new File(targetPath).isFile()) {
                    existsClassPath = Boolean.TRUE;
                    break outer;
                }
            }
        }

        if (!existsJavaPath) {
            this.fastNotice(project, PROJECT_TYPE_ERROR);
            return;
        }
        if (!existsClassPath) {
            this.fastNotice(project, COMPILE_PROJECT_ERROR);
            return;
        }

        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new JavaClassTransferable(targetPath), null);

        // copy successfully
        this.fastNotice(project, COPY_SUCCESSFULLY);
    }

    private void fastNotice(Project project, String msg) {
        final Notification notification = new Notification(
                COPY_CLASS.getPluginId().getIdString(),
                COPY_CLASS.getName(),
                msg,
                NotificationType.INFORMATION);
        Notifications.Bus.notify(notification, project);
    }

    private static class JavaClassTransferable implements Transferable {

        private final String filePath;

        public JavaClassTransferable(String filePath) {
            this.filePath = filePath;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.javaFileListFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.javaFileListFlavor.equals(flavor);
        }

        @Override
        public @NotNull Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return List.of(new File(filePath));
        }
    }

}
