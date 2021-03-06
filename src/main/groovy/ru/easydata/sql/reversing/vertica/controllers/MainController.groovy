package ru.easydata.sql.reversing.vertica.controllers

import javafx.css.PseudoClass
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ButtonBar
import javafx.scene.control.Menu
import javafx.scene.control.MenuItem
import javafx.scene.control.RadioMenuItem
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component
import ru.easydata.sql.reversing.vertica.configurations.FXMLViewLoader
import ru.easydata.sql.reversing.vertica.configurations.UserProperties
import ru.easydata.sql.reversing.vertica.interfaces.Reversing

/**
 * @author Сергей Семыкин
 * @since 14.11.2017
 */
@Component
class MainController {
	private static final PseudoClass SELECTED_CLASS = PseudoClass.getPseudoClass("selected")

	@Autowired
	private MessageSource messageSource

	@Autowired
	private Dialog dialog

	@Autowired
	private FXMLViewLoader fxmlViewLoader

	@Autowired
	private Reversing reversing

	@Autowired
	private BuildController buildController

	@Autowired
	private DeployController deployController

	@Autowired
	private UserProperties userProperties

	@FXML
	private VBox vBoxMenuLeft

	@FXML
	private GridPane paneContext

	@FXML
	private Menu menuLocales

	@FXML
	private Menu menuOpenRecent

	@FXML
	private RadioMenuItem menuItemEn

	@FXML
	private RadioMenuItem menuItemRu

	@FXML
	private Button general

	private Stage stageBuild
	private Stage stageDeploy
	private Stage stageAbout

	@FXML
	void initialize() {
		switch (LocaleContextHolder.getLocale().getLanguage()) {
			case 'ru':
				menuItemRu.setSelected(true)
				break
			case 'en':
				menuItemEn.setSelected(true)
				break
		}

		this.loadMenuOpenRecent()

		general.pseudoClassStateChanged(SELECTED_CLASS, true)

		this.paneContext.getChildren().clear()

		Parent form = this.fxmlViewLoader.get(general.id)
		if (form != null) {
			this.paneContext.getChildren().add(form)
		}
	}

	private void loadMenuOpenRecent() {
		this.menuOpenRecent.getItems().clear()

		this.userProperties.openList.each {path ->
			MenuItem m = new MenuItem(path)
			m.setMnemonicParsing(false)

			m.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				void handle(ActionEvent e) {
					if (reversing.isEdit()) {
						alertSave() {reversing.openFile(new File(path))}
					} else {
						reversing.openFile(new File(path))
					}
				}
			})

			this.menuOpenRecent.getItems().add(m)
		}
	}

	@FXML
	void menuLeftAction(ActionEvent actionEvent) {
		Object source = actionEvent.getSource()

		if (!source instanceof Button) {
			return
		}

		vBoxMenuLeft.getChildren().each {Node n ->
			if (n instanceof Button) {
				n.pseudoClassStateChanged(SELECTED_CLASS, false)
			}
		}

		Button button = (Button) source

		button.pseudoClassStateChanged(SELECTED_CLASS, true)

		this.paneContext.getChildren().clear()

		Parent form = this.fxmlViewLoader.get(button.id)
		if (form != null) {
			this.paneContext.getChildren().add(form)
		}
	}

	@FXML
	void menuTopAction(ActionEvent actionEvent) {
		Object source = actionEvent.getSource()

		if (!source instanceof MenuItem) {
			return
		}

		MenuItem menuItem = (MenuItem) source

		switch (menuItem.id) {
			case 'menuItemNew':
				this.newFile()
				break
			case 'menuItemOpen':
				this.openFile()
				break
			case 'menuItemSave':
				this.saveFile()
				break
			case 'menuItemSaveAs':
				this.saveAsFile()
				break
			case 'menuItemClose':
				this.close()
				break
			case 'menuItemBuild':
				this.build()
				break
			case 'menuItemDeploy':
				this.deploy()
				break
			case 'menuItemAbout':
				this.about()
				break
			case 'menuItemRu':
				reversing.setLocale(new Locale('ru'))
				menuItemRu.setSelected(true)
				break
			case 'menuItemEn':
				reversing.setLocale(new Locale('en'))
				menuItemEn.setSelected(true)
				break
		}
	}

	private void alertSave(Closure func) {
		switch (this.dialog.alertSave()) {
			case ButtonBar.ButtonData.YES:
				if (this.saveFile()) {
					func()
				}
				break
			case ButtonBar.ButtonData.NO:
				func()
				break
			default:
				break
		}
	}

	private void newFile() {
		if (this.reversing.isEdit()) {
			this.alertSave() {this.reversing.newFile()}
		} else {
			this.reversing.newFile()
		}
	}

	private void openFile() {
		if (this.reversing.isEdit()) {
			this.alertSave() {this.reversing.openFile(this.dialog.showOpenDialog())}
		} else {
			this.reversing.openFile(this.dialog.showOpenDialog())
		}

		this.loadMenuOpenRecent()
	}

	private boolean saveFile() {
		try {
			this.reversing.saveFile()
		} catch (FileNotFoundException e) {
			File file = this.dialog.showSaveDialog()
			if (file != null) {
				this.reversing.saveAsFile(file)
			} else {
				return false
			}
		}
		return true
	}

	private void saveAsFile() {
		this.reversing.saveAsFile(this.dialog.showSaveDialog())
	}

	private void close() {
		this.reversing.close()
	}

	private void build() {
		Parent root = this.fxmlViewLoader.get('build')
		if (this.stageBuild == null) {
			this.stageBuild = this.newStageWindowModal(root, this.messageSource.getMessage('app.window.build.title', null, LocaleContextHolder.getLocale()))
			stageBuild.setOnCloseRequest({e ->
				buildController.onClose(e)
			})
			stageBuild.setOnShown({e ->
				buildController.onShown(e)
			})
		} else {
			this.stageBuild.getScene().setRoot(root)
		}

		this.stageBuild.show()
	}

	private void deploy() {
		Parent root = this.fxmlViewLoader.get('deploy')
		if (this.stageDeploy == null) {
			this.stageDeploy = this.newStageWindowModal(root, this.messageSource.getMessage('app.window.deploy.title', null, LocaleContextHolder.getLocale()))
			stageDeploy.setOnCloseRequest({e ->
				deployController.onClose(e)
			})
			stageDeploy.setOnShown({e ->
				deployController.onShown(e)
			})
		} else {
			this.stageDeploy.getScene().setRoot(root)
		}

		this.stageDeploy.show()
	}

	private void about() {
		Parent root = this.fxmlViewLoader.get('about')
		if (this.stageAbout == null) {
			this.stageAbout = this.newStageWindowModal(root, this.messageSource.getMessage('fxml.menu.top.help.about', null, LocaleContextHolder.getLocale()))
		} else {
			this.stageAbout.getScene().setRoot(root)
		}

		this.stageAbout.show()
	}

	private Stage newStageWindowModal(Parent root, String title) {
		Stage stage = new Stage()
		stage.setScene(new Scene(root))
		stage.setTitle(title)
		stage.initModality(Modality.WINDOW_MODAL)
		stage.initOwner(this.paneContext.getScene().getWindow())

		return stage
	}
}
