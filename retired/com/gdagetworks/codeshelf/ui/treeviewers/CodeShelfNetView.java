/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: CodeShelfNetView.java,v 1.2 2012/07/19 06:11:33 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ui.treeviewers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import com.gadgetworks.codeshelf.application.Util;
import com.gadgetworks.codeshelf.controller.IController;
import com.gadgetworks.codeshelf.model.dao.DAOException;
import com.gadgetworks.codeshelf.model.dao.IDAOListener;
import com.gadgetworks.codeshelf.model.domain.CodeShelfNetwork;
import com.gadgetworks.codeshelf.model.domain.ControlGroup;
import com.gadgetworks.codeshelf.model.domain.DomainObjectABC;
import com.gadgetworks.codeshelf.model.domain.PickTag;
import com.gadgetworks.codeshelf.model.domain.WirelessDevice;
import com.gadgetworks.codeshelf.ui.LocaleUtils;
import com.gadgetworks.codeshelf.ui.wizards.CodeShelfNetWizard;
import com.gadgetworks.codeshelf.ui.wizards.ControlGroupWizard;
import com.gadgetworks.codeshelf.ui.wizards.PickTagDeviceWizard;

// --------------------------------------------------------------------------
/**
 * PickTagView shows a treeviewer of the PickTags and their associate PickTagModules.
 * 
 * @author jeffw
 */
public final class CodeShelfNetView implements ISelectionChangedListener, IDoubleClickListener, MenuListener, IDAOListener {

	//	private static final Log			LOGGER				= LogFactory.getLog(PickTagView.class);

	private static final int				ID_COL_WIDTH		= 200;
	private static final int				DESC_COL_WIDTH		= 200;
	private static final int				DETAILS_COL_WIDTH	= 400;

	private static final Log				LOGGER				= LogFactory.getLog(CodeShelfNetView.class);

	private Shell							mShell;
	private TreeViewer						mTreeViewer;
	private Tree							mTree;
	private CodeShelfNetViewContentProvider	mCodeShelfNetworkViewContentProvider;
	private Menu							mPopup;
	private IController						mController;

	public CodeShelfNetView(final Composite inParent, final IController inController, final int inStyle) {

		mController = inController;
		mShell = inParent.getShell();

		mTreeViewer = new TreeViewer(inParent, SWT.SINGLE | SWT.FULL_SELECTION);
		mTree = mTreeViewer.getTree();

		mTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		mTree.setFont(Util.getFontRegistry().get(Util.CODESHELF_NET_VIEW_TEXT));
		mTree.setHeaderVisible(true);
		mTree.setLinesVisible(false);
		mTree.setBackground(Util.getColorRegistry().get(Util.BACKGROUND_COLOR));

		//setupToolTips();

		// Create a popup for the items in the person tree.
		mPopup = new Menu(mTree);
		mTree.setMenu(mPopup);
		mPopup.addMenuListener(this);

		// Create a drag source on the tree.
		DragSource dragSource = new DragSource(mTree, DND.DROP_MOVE);
		dragSource.setTransfer(new Transfer[] { LocalSelectionTransfer.getTransfer() });
		dragSource.addDragListener(new DragSourceAdapter() {

			public void dragStart(DragSourceEvent inEvent) {
				PersistABC persistentObject = (PersistABC) mTree.getSelection()[0].getData();
				if (!(persistentObject instanceof CodeShelfNetwork)) {
					inEvent.doit = true;

					LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
					//					if (transfer.isSupportedType(inEvent.dataType)) {
					// Set the data to be the first selected item's text
					inEvent.data = persistentObject;
					transfer.setSelection(getTreeSelection());
					//					}
				}
				LOGGER.info(persistentObject);
			}

			public void dragSetData(DragSourceEvent inEvent) {

				LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
				if (transfer.isSupportedType(inEvent.dataType)) {
					// Set the data to be the first selected item's text
					PersistABC persistentObject = (PersistABC) mTree.getSelection()[0].getData();
					inEvent.data = persistentObject;//.getPersistentId().toString();

					transfer.setSelection(getTreeSelection());
				}

			}
		});

		// Create the drop target on the tree.
		Transfer[] transfers = new Transfer[] { LocalSelectionTransfer.getTransfer() };
		mTreeViewer.addDropSupport(DND.DROP_MOVE, transfers, new PickTagViewDropAdapter(mTreeViewer));

		TreeColumn column = new TreeColumn(mTree, SWT.NONE);
		column.setWidth(ID_COL_WIDTH);
		column.setText(LocaleUtils.getStr("codeshelfview.id_col.label"));
		column.setResizable(true);
		column.setData(CodeShelfNetViewDecoratedLabelProvider.ID_COL);

		column = new TreeColumn(mTree, SWT.NONE);
		column.setWidth(DESC_COL_WIDTH);
		column.setText(LocaleUtils.getStr("codeshelfview.desc_col.label"));
		column.setResizable(true);
		column.setMoveable(true);
		column.setData(CodeShelfNetViewDecoratedLabelProvider.DESC_COL);

		column = new TreeColumn(mTree, SWT.NONE);
		column.setWidth(DETAILS_COL_WIDTH);
		column.setText(LocaleUtils.getStr("codeshelfview.details_col.label"));
		column.setResizable(true);
		column.setMoveable(true);
		column.setData(CodeShelfNetViewDecoratedLabelProvider.DETAILS_COL);

		mCodeShelfNetworkViewContentProvider = new CodeShelfNetViewContentProvider();

		mTreeViewer.setContentProvider(mCodeShelfNetworkViewContentProvider);
		mTreeViewer.setComparator(new CodeShelfNetViewSorter());
		mTreeViewer.setLabelProvider(new CodeShelfNetViewDecoratedLabelProvider(mTree,
			new CodeShelfNetViewLabelProvider(),
			new CodeShelfNetViewDecorator()));
		mTreeViewer.addSelectionChangedListener(this);
		mTreeViewer.addDoubleClickListener(this);

		mTreeViewer.setInput(CodeShelfNetViewContentProvider.PICKTAGVIEW_ROOT);
	}

	// --------------------------------------------------------------------------
	/**
	 * @return
	 */
	public IStructuredSelection getTreeSelection() {
		return (IStructuredSelection) (mTreeViewer.getSelection());
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inListener
	 */
	public void addSelectionChangedListener(ISelectionChangedListener inListener) {
		mTreeViewer.addSelectionChangedListener(inListener);
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	public void selectionChanged(SelectionChangedEvent inEvent) {
		IStructuredSelection selection = (IStructuredSelection) inEvent.getSelection();
		Object element = selection.getFirstElement();
		if (element instanceof PickTag) {
			//mParentAppWindow.switchToOwnerAccount((Account) element);
		}
	}

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IDoubleClickListener#doubleClick(org.eclipse.jface.viewers.DoubleClickEvent)
	 */
	public void doubleClick(DoubleClickEvent inEvent) {
		IStructuredSelection selection = (IStructuredSelection) inEvent.getSelection();
		Object element = selection.getFirstElement();
		if (element instanceof PickTag) {
			//mParentAppWindow.switchToOwnerAccount((Account) element);
		}
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.events.MenuListener#menuHidden(org.eclipse.swt.events.MenuEvent)
	 */
	public void menuHidden(MenuEvent inEvent) {

	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.events.MenuListener#menuShown(org.eclipse.swt.events.MenuEvent)
	 */
	public void menuShown(MenuEvent inMenuEvent) {

		// Get rid of existing menu items
		MenuItem[] items = mPopup.getItems();
		for (int i = 0; i < items.length; i++) {
			((MenuItem) items[i]).dispose();
		}

		// Figure out what, if anything the user selected.
		Object itemData = null;
		if (mTree.getSelectionCount() > 0) {
			// Add menu items for current selection
			TreeItem treeItem = mTree.getSelection()[0];
			if (treeItem != null) {
				itemData = ((TreeItem) treeItem).getData();
			}
		}

		if (itemData == null) {
			codeShelfNetworkMenu(null);
		} else if (itemData instanceof CodeShelfNetwork) {
			CodeShelfNetwork codeShelfNetwork = (CodeShelfNetwork) itemData;
			codeShelfNetworkMenu(codeShelfNetwork);
		} else if (itemData instanceof ControlGroup) {
			ControlGroup controlGroup = (ControlGroup) itemData;
			controlGroupMenu(controlGroup);
		} else if (itemData instanceof PickTag) {
			PickTag pickTag = (PickTag) itemData;
			pickTagMenu(pickTag);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * This is the popup menu when the user clicks on a PickTagModule in the PickTagView.
	 * 
	 * @param inCodeShelfNetwork
	 */
	private void codeShelfNetworkMenu(final CodeShelfNetwork inCodeShelfNetwork) {

		// Add CodeShelfNetwork menu item.
		final MenuItem addItem = new MenuItem(mPopup, SWT.NONE);
		addItem.setText(LocaleUtils.getStr("codeshelfview.menu.add_codeshelfnetwork"));
		addItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event inEvent) {
				CodeShelfNetWizard.addCodeShelfNetwork(inCodeShelfNetwork);
			}
		});

		// Edit CodeShelfNetwork menu item.
		final MenuItem editItem = new MenuItem(mPopup, SWT.NONE);
		editItem.setText(LocaleUtils.getStr("codeshelfview.menu.edit_codeshelfnetwork"));
		editItem.setData(inCodeShelfNetwork);
		editItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event inEvent) {
				CodeShelfNetWizard.editCodeShelfNetwork(inCodeShelfNetwork);
			}
		});

		// Delete CodeShelfNetwork menu item.
		final MenuItem deleteItem = new MenuItem(mPopup, SWT.NONE);
		deleteItem.setText(LocaleUtils.getStr("codeshelfview.menu.delete_codeshelfnetwork"));
		deleteItem.setData(inCodeShelfNetwork);
		deleteItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event inEvent) {
				boolean deletePickTagModule = MessageDialog.openQuestion(Display.getCurrent().getActiveShell(),
					LocaleUtils.getStr("codeshelfview.menu.delete_codeshelfnetwork.title"),
					LocaleUtils.getStr("codeshelfview.menu.delete_codeshelfnetwork.prompt",
						new String[] { inCodeShelfNetwork.getDescription() }));
				if (deletePickTagModule) {
					try {
						CodeShelfNetwork.DAO.delete(inCodeShelfNetwork);
					} catch (DAOException e) {
						LOGGER.error(e);
					}
				}
			}
		});

		if (inCodeShelfNetwork != null) {
			new MenuItem(mPopup, SWT.SEPARATOR);

			// Add ControlGroup menu item.
			final MenuItem addCtrlItem = new MenuItem(mPopup, SWT.NONE);
			addCtrlItem.setText(LocaleUtils.getStr("codeshelfview.menu.add_controlgroup"));
			addCtrlItem.setData(inCodeShelfNetwork);
			addCtrlItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event inEvent) {
					ControlGroupWizard.addControlGroup(inCodeShelfNetwork);
				}
			});

		}

		editItem.setEnabled(true);
		deleteItem.setEnabled(true);
	}

	// --------------------------------------------------------------------------
	/**
	 * This is the popup menu when the user clicks on a PickTagModule in the PickTagView.
	 * 
	 * @param inControlGroup
	 */
	private void controlGroupMenu(final ControlGroup inControlGroup) {

		// Edit ControlGroup menu item.
		final MenuItem editItem = new MenuItem(mPopup, SWT.NONE);
		editItem.setText(LocaleUtils.getStr("codeshelfview.menu.edit_controlgroup"));
		editItem.setData(inControlGroup);
		editItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event inEvent) {
				ControlGroupWizard.editControlGroup(inControlGroup);
			}
		});

		// Delete ControlGroup menu item.
		final MenuItem deleteItem = new MenuItem(mPopup, SWT.NONE);
		deleteItem.setText(LocaleUtils.getStr("codeshelfview.menu.delete_controlgroup"));
		deleteItem.setData(inControlGroup);
		deleteItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event inEvent) {
				boolean deletePickTagModule = MessageDialog.openQuestion(Display.getCurrent().getActiveShell(),
					LocaleUtils.getStr("codeshelfview.menu.delete_controlgroup.title"),
					LocaleUtils.getStr("codeshelfview.menu.delete_controlgroup.prompt",
						new String[] { inControlGroup.getDescription() }));
				if (deletePickTagModule) {
					try {
						ControlGroup.DAO.delete(inControlGroup);
					} catch (DAOException e) {
						LOGGER.error(e);
					}
				}
			}
		});

		new MenuItem(mPopup, SWT.SEPARATOR);

		// Add PickTag menu item.
		final MenuItem adItem = new MenuItem(mPopup, SWT.NONE);
		adItem.setText(LocaleUtils.getStr("codeshelfview.menu.add_picktag"));
		adItem.setData(inControlGroup.getParentCodeShelfNetwork());
		adItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event inEvent) {
				PickTagDeviceWizard.createPickTagDevice(inControlGroup, mShell, mController);
			}
		});

		editItem.setEnabled(true);
		deleteItem.setEnabled(true);
	}

	// --------------------------------------------------------------------------
	/**
	 * This is the popup menu when the user clicks on an PickTag in the PickTagView.
	 * 
	 * @param inPickTag
	 */
	private void pickTagMenu(final PickTag inPickTag) {

		final MenuItem editItem = new MenuItem(mPopup, SWT.NONE);
		editItem.setText(LocaleUtils.getStr("codeshelfview.menu.edit_picktag"));
		editItem.setData(inPickTag);
		editItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event inEvent) {
				PickTag pickTag = (PickTag) WirelessDevice.DAO.loadByPersistentId(inPickTag.getPersistentId());
				PickTagDeviceWizard.editPickTagDevice(pickTag, mController, mShell);
			}
		});

		final MenuItem deleteItem = new MenuItem(mPopup, SWT.NONE);
		deleteItem.setText(LocaleUtils.getStr("codeshelfview.menu.delete_picktag"));
		deleteItem.setData(inPickTag);
		deleteItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event inEvent) {
				boolean deletePickTag = MessageDialog.openQuestion(Display.getCurrent().getActiveShell(),
					LocaleUtils.getStr("codeshelfview.menu.delete_picktag.title"),
					LocaleUtils.getStr("codeshelfview.menu.delete_picktag.prompt", new String[] { inPickTag.getDescription() }));
				if (deletePickTag) {
					try {
						WirelessDevice.DAO.delete(inPickTag);
					} catch (DAOException e) {
						LOGGER.error("", e);
					}
				}
			}
		});

		new MenuItem(mPopup, SWT.SEPARATOR);

		// Add module menu item.
		final MenuItem adItem = new MenuItem(mPopup, SWT.NONE);
		adItem.setText(LocaleUtils.getStr("codeshelfview.menu.add_picktagmodule"));
		adItem.setData(inPickTag);
		adItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event inEvent) {
				//				RulesetWizard.addPickTagRuleset(inPickTag);
			}
		});

		// Decide which menu items should be enabled.
		if (inPickTag == null) {
			editItem.setEnabled(false);
			deleteItem.setEnabled(false);
		} else {
			editItem.setEnabled(true);
			deleteItem.setEnabled(true);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 */
	public void switchToSelection() {
		if (mTreeViewer.getTree().getSelectionCount() > 0) {
			Object object = mTreeViewer.getTree().getSelection()[0].getData();
			if (object instanceof PickTag) {
				//mParentAppWindow.switchToOwnerAccount((Account) object);
			}
		}
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.gadgetworks.codeshelf.model.dao.IDAOListener#objectAdded(java.lang.Object)
	 */
	public void objectAdded(Object inObject) {
		if (doesDAOChangeApply(inObject)) {
			if (inObject instanceof CodeShelfNetwork) {
				mTreeViewer.add(CodeShelfNetViewContentProvider.PICKTAGVIEW_ROOT, inObject);
			} else if (inObject instanceof ControlGroup) {
				ControlGroup controlGroup = (ControlGroup) inObject;
				CodeShelfNetwork codeShelfNetwork = controlGroup.getParentCodeShelfNetwork();
				if (codeShelfNetwork != null) {
					mTreeViewer.add(codeShelfNetwork, inObject);
				}
			} else if (inObject instanceof PickTag) {
				PickTag pickTag = (PickTag) inObject;
				ControlGroup controlGroup = pickTag.getParentControlGroup();
				if (controlGroup != null) {
					mTreeViewer.add(controlGroup, inObject);
				}
			}
		}
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.gadgetworks.codeshelf.model.dao.IDAOListener#objectDeleted(java.lang.Object)
	 */
	public void objectDeleted(Object inObject) {
		if (doesDAOChangeApply(inObject)) {
			mTreeViewer.setSelection(null);
			mTreeViewer.remove(inObject);
		}
	}

	/*
	 * --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * 
	 * @see com.gadgetworks.codeshelf.model.dao.IDAOListener#objectUpdated(java.lang.Object)
	 */
	public void objectUpdated(Object inObject) {
		if (doesDAOChangeApply(inObject)) {
			Object refreshParentObject = inObject;

			if (inObject instanceof CodeShelfNetwork) {
				refreshParentObject = inObject;
			} else if (inObject instanceof ControlGroup) {
				refreshParentObject = ((ControlGroup) inObject).getParentCodeShelfNetwork();
			} else if (inObject instanceof PickTag) {
				refreshParentObject = ((PickTag) inObject).getParentControlGroup();
			}

			String[] properties = { CodeShelfNetViewSorter.PICKTAG_MACADDR_PROPERTY };
			mTreeViewer.update(inObject, properties);
			if (refreshParentObject != null) {
				mTreeViewer.refresh(refreshParentObject, true);
			}
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  @param inObject
	 *  @return
	 */
	private boolean doesDAOChangeApply(Object inObject) {
		boolean result = false;

		// We only want to update if it's an account or buddy update.
		if (inObject instanceof CodeShelfNetwork) {
			result = true;
		} else if (inObject instanceof ControlGroup) {
			result = true;
		} else if (inObject instanceof PickTag) {
			result = true;
		}
		return result;
	}

	/**
	 * @author jeffw
	 *
	 */
	private static class PickTagViewDropAdapter extends ViewerDropAdapter {

		public PickTagViewDropAdapter(final TreeViewer inTreeViewer) {
			super(inTreeViewer);
		}

		/* --------------------------------------------------------------------------
		 * (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ViewerDropAdapter#performDrop(java.lang.Object)
		 */
		public boolean performDrop(Object inDroppedObject) {

			if (inDroppedObject instanceof TreeSelection) {
				TreeSelection selection = (TreeSelection) inDroppedObject;
				Object first = selection.getFirstElement();
				if (first instanceof PickTag) {
					PickTag pickTag = (PickTag) first;
					ControlGroup parentControlGroup = pickTag.getParentControlGroup();
					if (getCurrentTarget() instanceof ControlGroup) {
						ControlGroup controlGroup = (ControlGroup) getCurrentTarget();
						controlGroup.addPickTag(pickTag);
						pickTag.setParentControlGroup(controlGroup);
						try {
							WirelessDevice.DAO.store(pickTag);
							ControlGroup.DAO.store(controlGroup);
						} catch (DAOException e) {
							LOGGER.error("", e);
						}
						ControlGroup.DAO.pushNonPersistentUpdates(parentControlGroup);
					}
				}
			}
			return true;
		}

		/* --------------------------------------------------------------------------
		 * (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ViewerDropAdapter#validateDrop(java.lang.Object, int, org.eclipse.swt.dnd.TransferData)
		 */
		public boolean validateDrop(Object inTarget, int inOperation, TransferData inTransferType) {
			boolean result = false;

			LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();

			ISelection selection = transfer.getSelection();
			if (selection instanceof TreeSelection) {
				TreeSelection treeSelection = (TreeSelection) selection;
				Object first = treeSelection.getFirstElement();
				if (first instanceof PickTag) {
					if (inTarget instanceof ControlGroup) {
						result = true;
					}
				} else if (first instanceof ControlGroup ) {
					if (inTarget instanceof CodeShelfNetwork) {
						result = true;
					}
				}
			}
			return result;
		}

		// --------------------------------------------------------------------------
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ViewerDropAdapter#dragOver(org.eclipse.swt.dnd.DropTargetEvent)
		 */
		@Override
		public void dragOver(DropTargetEvent inEvent) {
			super.dragOver(inEvent);
		}

	}
}
