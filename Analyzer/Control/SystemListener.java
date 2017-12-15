package Analyzer.Control;

import java.io.File;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Future;

import javax.swing.tree.DefaultMutableTreeNode;

import Analyzer.Model.FileNode;
import Analyzer.Model.FileTree;

/**
 * Class which allow to listen the system files changes
 * 
 * @author Valentin Bourcier
 */
public class SystemListener implements Runnable{
    
    public static SystemListener SYSTEM_LISTENER = new SystemListener();
    
    private FileTree tree;
    private CacheManager cache = CacheManager.getInstance();
    private int delay = 10000;
    private List<FileTreeListener> fileTreeListeners = new ArrayList<FileTreeListener>();
    
    
    /**
     * Method to set the tree that will be updated
     * @param tree FileTree to update on system changes
     */
    public void registerTree(FileTree tree){
        this.tree = tree;
    }
    
    /**
     * Method setting the refreshing delay
     * @param delay Refreshing delay in milliseconds
     */
    public void setDelay(int delay){
        this.delay = delay;
    }
    
    /**
     * Method to add a listener on FileTree updates
     * @param listener Instance of FileTreeListener
     */
    public void addFileTreeListener(FileTreeListener listener){
        this.fileTreeListeners.add(listener);
    }
    
    /**
     * Method throwing the update Event
     */
    public synchronized void throwUpdateEvent(int nbChanges){
        for (FileTreeListener fileTreeListener: fileTreeListeners) {
            fileTreeListener.fileTreeUpdated(this.tree, nbChanges);
        }
    }
    
    /**
     * Method listening on the system changes
     */
    @Override
    public void run() {
        System.out.println("System listener started");
        while (true) {
            try{
                Thread.sleep(this.delay);
            }catch(InterruptedException error){
                System.out.println("System listening interrupted.");
            }
            boolean changed = false;
            int nbChanges = 0;
            @SuppressWarnings("unchecked")
			Enumeration<DefaultMutableTreeNode> en = this.tree.getRoot().preorderEnumeration();
            while (en.hasMoreElements()) {
                DefaultMutableTreeNode node = en.nextElement();
                FileNode file = this.tree.getFileNode(node);
                String path = file.getAbsolutePath();
                File sysFile = new File(path);
                if(!sysFile.exists()) {
                	cache.remove(path);
                	node.removeAllChildren();
                	node.removeFromParent();
                	node = null;
                	changed = true;
                	nbChanges++;
                }
                if(cache.contains(path) && cache.get(path).INSTANCE_TIME < sysFile.lastModified()){
                    node.setUserObject(this.cache.getMoreRecent(path));
                    this.cache.add(this.cache.getMoreRecent(path));
                    changed = true;
                    nbChanges++;
                }
                
               	if(sysFile.isDirectory() && sysFile.listFiles().length > node.getChildCount()) {
               		this.cache.clean();
               		String directory = this.tree.getFileNode(this.tree.getRoot()).getAbsolutePath();
               		this.tree = new FileTree();
               		Future<DefaultMutableTreeNode> result = this.tree.buildFileTree(directory, false, true, 0);
               		while(!result.isDone()) {}
                    changed = true;
                    nbChanges++;
                }
            }
            
            if(changed){
                throwUpdateEvent(nbChanges);
            }
        }
    }   
}