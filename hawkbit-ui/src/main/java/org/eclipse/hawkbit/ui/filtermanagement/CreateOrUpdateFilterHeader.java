/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import static java.util.stream.Collectors.toSet;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.components.SPUIButton;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.filtermanagement.event.CustomFilterUIEvent;
import org.eclipse.hawkbit.ui.filtermanagement.state.FilterManagementUIState;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.github.wolfie.popupextension.PopupExtension;
import com.google.common.base.Strings;
import com.vaadin.event.FieldEvents.BlurEvent;
import com.vaadin.event.FieldEvents.BlurListener;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.event.LayoutEvents.LayoutClickEvent;
import com.vaadin.event.LayoutEvents.LayoutClickListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 *
 *
 */
@SpringComponent
@ViewScope
public class CreateOrUpdateFilterHeader extends VerticalLayout implements Button.ClickListener {

    private static final long serialVersionUID = 7474232427119031474L;

    private static final String breadcrumbCustomFilters = "breadcrumb.target.filter.custom.filters";

	private static final int TOKEN_LENGTH = 20;

	private static final String DELIMITER = " ";

	//TODO: To be removed its just for testing.
	private final Set<SuggestionDto> SUGGESTIONS = new HashSet<SuggestionDto>();

    @Autowired
    private I18N i18n;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private FilterManagementUIState filterManagementUIState;

    @Autowired
    private transient TargetFilterQueryManagement targetFilterQueryManagement;

    @Autowired
    private SpPermissionChecker permissionChecker;

    @Autowired
    private UINotification notification;

    @Autowired
    private transient UiProperties uiProperties;

    @Autowired
    @Qualifier("uiExecutor")
    private transient Executor executor;

    private HorizontalLayout breadcrumbLayout;

    private Button breadcrumbButton;

    private Label breadcrumbName;

    private Label headerCaption;

    private TextField queryTextField;

    private TextField nameTextField;

    private Label nameLabel;

    private SPUIButton closeIcon;

    private Button saveButton;

    private Link helpLink;

    private Label validationIcon;

    private HorizontalLayout searchLayout;

    private String oldFilterName;

    private String oldFilterQuery;

    private HorizontalLayout titleFilterIconsLayout;

    private HorizontalLayout captionLayout;

    private BlurListener nameTextFieldBlusListner;

    private LayoutClickListener nameLayoutClickListner;

    private boolean validationFailed = false;
	
	private Window window;

	private int lastCursorValue;

	private int currentCursorValue;

	private VerticalLayout suggestions;

	private String queryForSuggestion = "";

	private String value = "";

	private String nextSuggestionValueType = "";
	//private String selectedSuggestionValueType = "";

	private PopupExtension popupExtension;

    /**
     * Initialize the Campaign Status History Header.
     */
    @PostConstruct
    public void init() {
        createComponents();
        createListeners();
        buildLayout();
        restoreOnLoad();
        setUpCaptionLayout(filterManagementUIState.isCreateFilterViewDisplayed());
        eventBus.subscribe(this);
    }

    private void restoreOnLoad() {
        if (filterManagementUIState.isEditViewDisplayed()) {
            populateComponents();
        }
    }

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final CustomFilterUIEvent custFUIEvent) {
        if (custFUIEvent == CustomFilterUIEvent.TARGET_FILTER_DETAIL_VIEW) {
            populateComponents();
            eventBus.publish(this, CustomFilterUIEvent.TARGET_DETAILS_VIEW);
        } else if (custFUIEvent == CustomFilterUIEvent.CREATE_NEW_FILTER_CLICK) {
            setUpCaptionLayout(true);
            resetComponents();
        } else if (custFUIEvent == CustomFilterUIEvent.UPDATE_TARGET_FILTER_SEARCH_ICON) {
            UI.getCurrent().access(() -> updateStatusIconAfterTablePopulated());
        }
    }

    private void populateComponents() {
        if (filterManagementUIState.getTfQuery().isPresent()) {
            queryTextField.setValue(filterManagementUIState.getTfQuery().get().getQuery());
            nameLabel.setValue(filterManagementUIState.getTfQuery().get().getName());
            oldFilterName = filterManagementUIState.getTfQuery().get().getName();
            oldFilterQuery = filterManagementUIState.getTfQuery().get().getQuery();
        }
        breadcrumbName.setValue(nameLabel.getValue());
        showValidationSuccesIcon();
        titleFilterIconsLayout.addStyleName(SPUIStyleDefinitions.TARGET_FILTER_CAPTION_LAYOUT);
        headerCaption.setVisible(false);
        setUpCaptionLayout(false);
    }

    private void resetComponents() {
        headerCaption.setVisible(true);
        breadcrumbName.setValue(headerCaption.getValue());
        nameLabel.setValue("");
        queryTextField.setValue("");
        setInitialStatusIconStyle(validationIcon);
        validationFailed = false;
        saveButton.setEnabled(false);
        titleFilterIconsLayout.removeStyleName(SPUIStyleDefinitions.TARGET_FILTER_CAPTION_LAYOUT);
    }

    private Label createStatusIcon() {
        final Label statusIcon = new Label();
        statusIcon.setImmediate(true);
        statusIcon.setContentMode(ContentMode.HTML);
        statusIcon.setSizeFull();
        setInitialStatusIconStyle(statusIcon);
        statusIcon.setId(SPUIComponetIdProvider.VALIDATION_STATUS_ICON_ID);
        return statusIcon;
    }

    private void setInitialStatusIconStyle(final Label statusIcon) {
        statusIcon.setValue(FontAwesome.CHECK_CIRCLE.getHtml());
        statusIcon.setStyleName("hide-status-label");
    }

    private void createComponents() {

        breadcrumbButton = createBreadcrumbButton();

        headerCaption = SPUIComponentProvider.getLabel(SPUILabelDefinitions.VAR_CREATE_FILTER,
                SPUILabelDefinitions.SP_WIDGET_CAPTION);

        nameLabel = SPUIComponentProvider.getLabel("", SPUILabelDefinitions.SP_LABEL_SIMPLE);
        nameLabel.setId(SPUIComponetIdProvider.TARGET_FILTER_QUERY_NAME_LABEL_ID);

        nameTextField = createNameTextField();
        nameTextField.setWidth(380, Unit.PIXELS);

        queryTextField = createSearchField();
        addSearchLisenter();

        validationIcon = createStatusIcon();
        saveButton = createSaveButton();

        helpLink = SPUIComponentProvider.getHelpLink(uiProperties.getLinks().getDocumentation().getTargetfilterView());

        closeIcon = createSearchResetIcon();
		
		window = getWindow();
		suggestions = new VerticalLayout();
		window.setContent(suggestions);
		UI.getCurrent().addWindow(window);
    }

    private Button createBreadcrumbButton() {
        final Button createFilterViewLink = SPUIComponentProvider.getButton(null, "", "", null, false, null,
                SPUIButtonStyleSmallNoBorder.class);
        createFilterViewLink.setStyleName(ValoTheme.LINK_SMALL + " " + "on-focus-no-border link rollout-caption-links");
        createFilterViewLink.setDescription(i18n.get(breadcrumbCustomFilters));
        createFilterViewLink.setCaption(i18n.get(breadcrumbCustomFilters));
        createFilterViewLink.addClickListener(value -> showCustomFiltersView());

        return createFilterViewLink;
    }

    private TextField createNameTextField() {
        final TextField nameField = SPUIComponentProvider.getTextField("", ValoTheme.TEXTFIELD_TINY, false, null,
                i18n.get("textfield.customfiltername"), true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        nameField.setId(SPUIComponetIdProvider.CUSTOM_FILTER_ADD_NAME);
        nameField.setPropertyDataSource(nameLabel);
        nameField.addTextChangeListener(event -> onFilterNameChange(event));
        return nameField;
    }

    private void createListeners() {
        nameTextFieldBlusListner = new BlurListener() {
            private static final long serialVersionUID = -2300955622205082213L;

            @Override
            public void blur(final BlurEvent event) {
                if (!Strings.isNullOrEmpty(nameTextField.getValue())) {
                    captionLayout.removeComponent(nameTextField);
                    captionLayout.addComponent(nameLabel);
                }
            }
        };
        nameLayoutClickListner = new LayoutClickListener() {
            private static final long serialVersionUID = 6188308537393130004L;

            @Override
            public void layoutClick(final LayoutClickEvent event) {
                if (event.getClickedComponent() instanceof Label) {
                    captionLayout.removeComponent(nameLabel);
                    captionLayout.addComponent(nameTextField);
                    nameTextField.focus();
                }
            }
        };
    }

    private void onFilterNameChange(final TextChangeEvent event) {
        if (isNameAndQueryEmpty(event.getText(), queryTextField.getValue())
                || (event.getText().equals(oldFilterName) && queryTextField.getValue().equals(oldFilterQuery))) {
            saveButton.setEnabled(false);
        } else {
            if (hasSavePermission()) {
                saveButton.setEnabled(true);
            }
        }
    }

    private void buildLayout() {
        captionLayout = new HorizontalLayout();
        captionLayout.setDescription(i18n.get("tooltip.click.to.edit"));
        captionLayout.setId(SPUIComponetIdProvider.TARGET_FILTER_QUERY_NAME_LAYOUT_ID);

        titleFilterIconsLayout = new HorizontalLayout();
        titleFilterIconsLayout.addComponents(headerCaption, captionLayout);
        titleFilterIconsLayout.setSpacing(true);

        breadcrumbLayout = new HorizontalLayout();
        breadcrumbLayout.addComponent(breadcrumbButton);
        breadcrumbLayout.addComponent(new Label(">"));
        breadcrumbName = SPUIComponentProvider.getLabel(null, SPUILabelDefinitions.SP_WIDGET_CAPTION);
        breadcrumbLayout.addComponent(breadcrumbName);
        breadcrumbName.addStyleName("breadcrumbPaddingLeft");

        final HorizontalLayout titleFilterLayout = new HorizontalLayout();
        titleFilterLayout.setSizeFull();
        titleFilterLayout.addComponents(titleFilterIconsLayout, closeIcon);
        titleFilterLayout.setExpandRatio(titleFilterIconsLayout, 1.0F);
        titleFilterLayout.setComponentAlignment(titleFilterIconsLayout, Alignment.TOP_LEFT);
        titleFilterLayout.setComponentAlignment(closeIcon, Alignment.TOP_RIGHT);

        validationIcon = createStatusIcon();

        searchLayout = new HorizontalLayout();
        searchLayout.setSizeUndefined();
        searchLayout.setSpacing(false);
        searchLayout.addComponents(validationIcon, queryTextField);
        searchLayout.addStyleName("custom-search-layout");
        searchLayout.setComponentAlignment(validationIcon, Alignment.TOP_CENTER);
		
		popupExtension = PopupExtension.extend(searchLayout);
		popupExtension.setAnchor(Alignment.BOTTOM_LEFT);
		popupExtension.setDirection(Alignment.BOTTOM_RIGHT);

        final HorizontalLayout iconLayout = new HorizontalLayout();
        iconLayout.setSizeUndefined();
        iconLayout.setSpacing(false);
        iconLayout.addComponents(helpLink, saveButton);

        final HorizontalLayout queryLayout = new HorizontalLayout();
        queryLayout.setSizeUndefined();
        queryLayout.setSpacing(true);
        queryLayout.addComponents(searchLayout, iconLayout);

        addComponent(breadcrumbLayout);
        addComponent(titleFilterLayout);
        addComponent(queryLayout);
        setSpacing(true);
        addStyleName(SPUIStyleDefinitions.WIDGET_TITLE);
        addStyleName("bordered-layout");
    }

    private void setUpCaptionLayout(final boolean isCreateView) {
        captionLayout.removeAllComponents();
        if (isCreateView) {
            nameTextField.removeBlurListener(nameTextFieldBlusListner);
            captionLayout.removeLayoutClickListener(nameLayoutClickListner);
            captionLayout.addComponent(nameTextField);
        } else {
            captionLayout.addComponent(nameLabel);
            nameTextField.addBlurListener(nameTextFieldBlusListner);
            captionLayout.addLayoutClickListener(nameLayoutClickListner);
        }
    }

    private void addSearchLisenter() {
            queryTextField.addTextChangeListener(new TextChangeListener() {
			private static final long serialVersionUID = -6668604418942689391L;

			@Override
			public void textChange(final TextChangeEvent event) {
				hideWindow();
				currentCursorValue = queryTextField.getCursorPosition();
				System.out.println("currentCursorValue = "+currentCursorValue);
				suggestions.removeAllComponents();
				int windowPosition = event.getCursorPosition()*5;
				if(windowPosition >= 145){
					windowPosition = 145;
				}
				window.setPositionX(windowPosition);
				isDeleted(event.getText());
				prepareQueryForSuggestion(event.getText());
				if (!event.getText().isEmpty() /*&& !queryForSuggestion.isEmpty()*/) {
					//if(!(nextSuggestionValueType.equals("LOGICAL_OPERATORS") &&  event.getText().isEmpty())){
					prepareSuggestionList();
					//}
					Set<SuggestionDto> suggestionDtos = getSuggestions(queryForSuggestion);
					for (SuggestionDto suggestionDto : suggestionDtos) {
						System.out.println("list of suggestions = "+suggestionDto.getDisplayString());
						Button suggestion = new Button(suggestionDto.getDisplayString());
						suggestion.addStyleName(ValoTheme.BUTTON_TINY + DELIMITER + ValoTheme.BUTTON_BORDERLESS
								+ DELIMITER + "custom-button");
						suggestion.addClickListener(new Button.ClickListener() {

							private static final long serialVersionUID = 1L;

							@Override
							public void buttonClick(ClickEvent clickEvent) {
								selectionChanged(clickEvent);
								System.out.println("value = " + value);
								queryTextField.setValue(value);
								System.out.println(clickEvent.getButton().getCaption());
								suggestions.removeAllComponents();
								queryTextField.setCursorPosition(value.length());
								lastCursorValue = queryTextField.getCursorPosition();
								queryForSuggestion = "";
								//setSelectedSuggestionValueType();
								//suggestionSelectedMap.put(queryTextField.getCursorPosition(), new SuggestionSelected(selectedSuggestionValueType, clickEvent.getButton().getCaption()));
								hideWindow();
							}


						});
						suggestions.addComponent(suggestion);
						suggestions.setComponentAlignment(suggestion, Alignment.MIDDLE_LEFT);
					}

					if (!suggestionDtos.isEmpty()) {
						showWindow(suggestions);
					} 
				}

				validationIcon.addStyleName("show-status-label");
				showValidationInProgress();
				onQueryChange(event.getText());
				executor.execute(new StatusCircledAsync(UI.getCurrent()));
			}

        });
    }

	/*private void setSelectedSuggestionValueType() {
		if(nextSuggestionValueType.equals("TOKENS")){
			selectedSuggestionValueType = "TOKENS";
		}else if(nextSuggestionValueType.equals("RELATIONAL_OPERATORS")){
			selectedSuggestionValueType = "RELATIONAL_OPERATORS";
		}else if(nextSuggestionValueType.equals("VALUES")){
			selectedSuggestionValueType = "VALUES";
		}else if(nextSuggestionValueType.equals("LOGICAL_OPERATORS")){
			selectedSuggestionValueType = "LOGICAL_OPERATORS";
		}

	}*/

    class StatusCircledAsync implements Runnable {
        private final UI current;

        public StatusCircledAsync(final UI current) {
            this.current = current;
        }

        @Override
        public void run() {
            UI.setCurrent(current);
            eventBus.publish(this, CustomFilterUIEvent.ON_FILTER_QUERY_EDIT);
        }
    }

    private void onQueryChange(final String input) {
        if (!Strings.isNullOrEmpty(input)) {
            final ValidationResult validationResult = FilterQueryValidation.getExpectedTokens(input);
            if (!validationResult.getIsValidationFailed()) {
                filterManagementUIState.setFilterQueryValue(input);
                filterManagementUIState.setIsFilterByInvalidFilterQuery(Boolean.FALSE);
                validationFailed = false;
            } else {
                validationFailed = true;
                filterManagementUIState.setFilterQueryValue(null);
                filterManagementUIState.setIsFilterByInvalidFilterQuery(Boolean.TRUE);
                validationIcon.setDescription(validationResult.getMessage());
                showValidationFailureIcon();
            }
            enableDisableSaveButton(validationFailed, input);
        } else {
            setInitialStatusIconStyle(validationIcon);
            filterManagementUIState.setFilterQueryValue(null);
            filterManagementUIState.setIsFilterByInvalidFilterQuery(Boolean.TRUE);
        }
        queryTextField.setValue(input);
    }


	/**
	 * case 1: To get the suggestion list initially
	 * case 2: if the currentCursorValue <= lastCursorValue i.e something deleted from the middle of the query in the textfield get the appropriate suggestion list
	 * case 3: To get the suggestion list based on the type only after the word enetered in the textfield is completed.Note: The word is considered completed only when user gives the whitespace after the word.
	 */
	private void prepareSuggestionList() {
		//    	int i = 1;
		//    	CITY_SUGGESTIONS.clear();
		//		for(String expectedToken : expectedTokens){
		//			CITY_SUGGESTIONS.add(new SuggestionDto(i, expectedToken));
		//			i++;
		//		}


		if(nextSuggestionValueType.isEmpty()) {
			SUGGESTIONS.clear();
			nextSuggestionValueType = "RELATIONAL_OPERATORS";
			SUGGESTIONS.addAll(SuggestionsListsFactory.getTOKENS());
		}
		if (currentCursorValue <= lastCursorValue) {
			System.out.println("lastCursorValue = " + lastCursorValue);
			

			//			for(Integer selectedCursorPos : suggestionSelectedMap.keySet()){
			//				if(currentCursorValue <= selectedCursorPos){
			String typeOfSuggestion = calculateTypeOfSuggestion();
			if(typeOfSuggestion != null){
				SUGGESTIONS.clear();
				System.out.println("typeOfSuggestion = "+typeOfSuggestion);
				SUGGESTIONS.addAll(SuggestionsListsFactory.getSuggestionListFromType(typeOfSuggestion));
			}
			
			//				}
			//			}		
		}else if(isCompleteWord()){
			System.out.println("nextSuggestionValueType = "+nextSuggestionValueType);
			SUGGESTIONS.clear();
			if(nextSuggestionValueType.isEmpty() || nextSuggestionValueType.equals("TOKENS")){
				//nextSuggestionValueType = "TOKENS";
				nextSuggestionValueType = "RELATIONAL_OPERATORS";
				SUGGESTIONS.addAll(SuggestionsListsFactory.getTOKENS());
			}else if(nextSuggestionValueType.equals("RELATIONAL_OPERATORS")){
				//nextSuggestionValueType = "RELATIONAL_OPERATORS";
				nextSuggestionValueType = "VALUES";
				SUGGESTIONS.addAll(SuggestionsListsFactory.getRELATIONAL_OPERATORS());
			}else if(nextSuggestionValueType.equals("VALUES")){
				//nextSuggestionValueType = "VALUES";
				nextSuggestionValueType = "LOGICAL_OPERATORS";
				SUGGESTIONS.addAll(SuggestionsListsFactory.getValues());
			}else if(nextSuggestionValueType.equals("LOGICAL_OPERATORS")){
				nextSuggestionValueType = "TOKENS";
				SUGGESTIONS.addAll(SuggestionsListsFactory.getLOGICAL_OPERATORS());
			}
		}

		//SUGGESTIONS.addAll(SuggestionsListsFactory.getSuggestionListFromType(previousSuggestionValueType));

	}

	
	/**
	 * @return value eneterd in the textfield is a complete word or not based on the whitespace after the word
	 */
	private boolean isCompleteWord() {
		System.out.println("isCompleteWord "+value.endsWith(" "));
		return value.endsWith(" ");
	}

	/**
	 * @return the type of suggestion to populate the suggestion list
	 */
	private String calculateTypeOfSuggestion() {
		int i = 1;
		int index = 0;
		String suggestionType = null;
		while(index < queryTextField.getValue().lastIndexOf(" ")+1){
			index = queryTextField.getValue().indexOf(" " , index)+1;
			if(i == 5){
				i =1;
			}
			if(queryTextField.getCursorPosition() <= index){
				switch (i) {
				case 1:
					suggestionType = "TOKENS";
					break;
				case 2:
					suggestionType =  "RELATIONAL_OPERATORS";
					break;
				case 3:
					suggestionType = "VALUES";
					break;
				case 4:
					suggestionType = "LOGICAL_OPERATORS";
					break;
				default:
					break;
				}
				break;
			}
			i++;
		}
		//for type as tokens
		//		int indexForTokens = queryTextField.getValue().indexOf(" ", 0)+1;
		//		//for type as relationalOperators
		//		int indexForRelationalOperators = queryTextField.getValue().indexOf(" ", indexForTokens)+1;
		//		//for type as value
		//		int indexForValues = queryTextField.getValue().indexOf(" ", indexForRelationalOperators)+1;
		//		//for type as relationalOperators
		//	    int indexForLogicalOperators = queryTextField.getValue().indexOf(" ", indexForValues)+1;
		//		if(queryTextField.getCursorPosition() <= index){
		//			SUGGESTIONS.addAll(SuggestionsListsFactory.getSuggestionListFromType("TOKENS"));
		//		}else if(queryTextField.getCursorPosition() <= queryTextField.getValue().indexOf(" ", index))
		//return suggestionType;
		return suggestionType;

	}
	
    private void enableDisableSaveButton(final boolean validationFailed, final String query) {
        if (validationFailed || (isNameAndQueryEmpty(nameTextField.getValue(), query)
                || (query.equals(oldFilterQuery) && nameTextField.getValue().equals(oldFilterName)))) {
            saveButton.setEnabled(false);
        } else {
            if (hasSavePermission()) {
                saveButton.setEnabled(true);
            }
        }
    }

    private static boolean isNameAndQueryEmpty(final String name, final String query) {
        if (Strings.isNullOrEmpty(name) && Strings.isNullOrEmpty(query)) {
            return true;
        }
        return false;
    }

    private void showValidationSuccesIcon() {
        validationIcon.setValue(FontAwesome.CHECK_CIRCLE.getHtml());
        validationIcon.setStyleName(SPUIStyleDefinitions.SUCCESS_ICON);
    }

    private void showValidationFailureIcon() {
        validationIcon.setValue(FontAwesome.TIMES_CIRCLE.getHtml());
        validationIcon.setStyleName(SPUIStyleDefinitions.ERROR_ICON);
    }

    private void showValidationInProgress() {
        validationIcon.setValue(null);
        validationIcon.setStyleName(SPUIStyleDefinitions.TARGET_FILTER_SEARCH_PROGRESS_INDICATOR_STYLE);
    }

    private SPUIButton createSearchResetIcon() {
        final SPUIButton button = (SPUIButton) SPUIComponentProvider.getButton("create.custom.filter.close.Id", "", "",
                null, false, FontAwesome.TIMES, SPUIButtonStyleSmallNoBorder.class);
        button.addClickListener(event -> closeFilterLayout());
        return button;
    }

    private TextField createSearchField() {
        final TextField textField = SPUIComponentProvider.getTextField("", ValoTheme.TEXTFIELD_TINY, false, "", "",
                true, SPUILabelDefinitions.TARGET_FILTER_QUERY_TEXT_FIELD_LENGTH);
        textField.setId("custom.query.text.Id");
        textField.addStyleName("target-filter-textfield");
        textField.setWidth(900.0F, Unit.PIXELS);
        textField.setTextChangeEventMode(TextChangeEventMode.EAGER);
        textField.setTextChangeTimeout(1000);

        textField.addShortcutListener(new AbstractField.FocusShortcut(textField, KeyCode.ENTER));
        return textField;
    }

    private void closeFilterLayout() {
        filterManagementUIState.setFilterQueryValue(null);
        filterManagementUIState.setCreateFilterBtnClicked(false);
        filterManagementUIState.setEditViewDisplayed(false);
        filterManagementUIState.setTfQuery(null);
        eventBus.publish(this, CustomFilterUIEvent.EXIT_CREATE_OR_UPDATE_FILTRER_VIEW);
    }

    private Button createSaveButton() {
        saveButton = SPUIComponentProvider.getButton(SPUIComponetIdProvider.CUSTOM_FILTER_SAVE_ICON,
                SPUIComponetIdProvider.CUSTOM_FILTER_SAVE_ICON, "Save", null, false, FontAwesome.SAVE,
                SPUIButtonStyleSmallNoBorder.class);
        saveButton.addClickListener(this);
        saveButton.setEnabled(false);
        return saveButton;
    }

	/**
	 * @return pop up window for displaying the suggestion list
	 */
	private Window getWindow() {
		Window tempwindow = new Window();
		tempwindow.addStyleName("custom-window");
		tempwindow.setModal(false);
		tempwindow.setClosable(false);
		tempwindow.setResizable(false);
		tempwindow.setDraggable(false);
		tempwindow.setWidth(100, Unit.PIXELS);
		tempwindow.setHeight(-1, Unit.PIXELS);
		tempwindow.setPosition(400, 200);
		tempwindow.setVisible(false);
		return tempwindow;
	}

	/**
	 * gets the filtered suggestion from the available suggestion based on the queryForSuggestion entered by the user
	 * @param queryForSuggestion
	 * @return
	 */
	private Set<SuggestionDto> getSuggestions(String queryForSuggestion) {
		if (!queryForSuggestion.trim().isEmpty()) {
			if (value != null && !value.isEmpty()) {
				String startIndexUptoCursorText = value.substring(0, queryTextField.getCursorPosition());
				String cursortToEndText = value.substring(queryTextField.getCursorPosition(), value.length());

				String[] firstPartTokens = startIndexUptoCursorText.split(DELIMITER);
				String[] finalTokenList = new String[TOKEN_LENGTH];
				int indexOFTokenToBeReplaced = firstPartTokens.length - 1;
				int j = 1;

				int finalListIndex = 0;
				for (String token : firstPartTokens) {
					finalTokenList[finalListIndex] = token;
					finalListIndex++;
				}

				if (!cursortToEndText.isEmpty() && !cursortToEndText.startsWith(DELIMITER)) {
					String[] secondPartTokens = cursortToEndText.split(DELIMITER);
					finalTokenList[finalListIndex - 1] = finalTokenList[finalListIndex - 1] + secondPartTokens[0];
					for (int i = 1; i < secondPartTokens.length; i++) {
						finalTokenList[finalListIndex] = secondPartTokens[i];
						finalListIndex++;
					}
				}
				return SUGGESTIONS.stream().filter(suggestion -> suggestion.getDisplayString().toLowerCase()
						.startsWith(finalTokenList[indexOFTokenToBeReplaced].toLowerCase())).collect(toSet());
			}
			// }
			String[] array = queryForSuggestion.split(DELIMITER);
			String toCompare = array[array.length - 1];
			return SUGGESTIONS.stream().filter(
					suggestion -> suggestion.getDisplayString().toLowerCase().startsWith(toCompare.toLowerCase()))
					.collect(toSet());
		}
		return new HashSet<>();
	}

	/**
	 * to check if anything is deleted from the available textfield value and set the proper cursor position
	 * @param textFieldText
	 */
	protected void isDeleted(String textFieldText) {
		if (!textFieldText.isEmpty()) {
			if (currentCursorValue < lastCursorValue) {
				System.out.println("lastCursorValue = " + lastCursorValue);
				//				for(Integer selectedCursorPos : suggestionSelectedMap.keySet()){
				//					if(currentCursorValue < selectedCursorPos){
				//						SUGGESTIONS.addAll(SuggestionsListsFactory.getSuggestionListFromType(suggestionSelectedMap.get(selectedCursorPos).getSelectionType()));
				//					}
				//				}
				queryTextField.setCursorPosition(currentCursorValue);
			}
		} else {
			lastCursorValue = 0;
			currentCursorValue = 0;
			nextSuggestionValueType = "";
		}
		value = textFieldText;
	}

	/**
	 * prepare the query enetred by the user again which suggestions will be filtered from the available suggestion list
	 * @param textFieldValue
	 */
	private void prepareQueryForSuggestion(String textFieldValue) {
		if (currentCursorValue == textFieldValue.length()) {
			int beginIndex = textFieldValue.lastIndexOf(DELIMITER);
			if (beginIndex != -1) {
				queryForSuggestion = textFieldValue.substring(beginIndex, currentCursorValue).trim();
				System.out.println("query for suggestion " + queryForSuggestion);
				// lastCursorValue = currentCursorValue;

			} else {
				lastCursorValue = currentCursorValue;
				queryForSuggestion = textFieldValue.substring(0, currentCursorValue).trim();
			}

		} else {
			int beginIndex = textFieldValue.substring(0, currentCursorValue).trim().lastIndexOf(DELIMITER);
			if (beginIndex == -1) {
				beginIndex = 0;
			}
			queryForSuggestion = textFieldValue.substring(beginIndex, currentCursorValue).trim();
			System.out.println("query for suggestion for in mid words " + queryForSuggestion);
		}

	}

	/**
	 * prepare the value to be set in the textfield in the right position within the textfield after the user selected the suggestion from the available suggestion list
	 * @param clickEvent
	 */
	protected void selectionChanged(ClickEvent clickEvent) {
		String evaluatedString = "";
		String selectedOptionText = clickEvent.getButton().getCaption();
		if (value != null && !value.isEmpty()) {
			String startIndexUptoCursorText = value.substring(0, queryTextField.getCursorPosition());
			String cursortToEndText = value.substring(queryTextField.getCursorPosition(), value.length());

			String[] firstPartTokens = startIndexUptoCursorText.split(DELIMITER);
			firstPartTokens[firstPartTokens.length - 1] = clickEvent.getButton().getCaption();
			for (String val : firstPartTokens) {
//				if(!val.equals(firstPartTokens[firstPartTokens.length - 1])){
//					evaluatedString = evaluatedString + val + DELIMITER;
//				} else {
					evaluatedString = evaluatedString + val+ DELIMITER;
//				}

			}

			if (!cursortToEndText.isEmpty() && cursortToEndText.startsWith(DELIMITER)) {
				// next text after ursr.user is trying to get suggestions of
				// last token
				evaluatedString = evaluatedString.trim()
						+ value.substring(queryTextField.getCursorPosition(), value.length());
			} else {
				// skip the text after cursor
				if (!cursortToEndText.isEmpty()) {
					String[] secondPartTokens = cursortToEndText.split(DELIMITER);
					for (int i = 1; i < secondPartTokens.length; i++) {
						evaluatedString = evaluatedString + secondPartTokens[i] /*+ DELIMITER*/;
					}
				}
			}
			if (!evaluatedString.isEmpty()) {
				value = evaluatedString.trim();
			} else {
				value = value + selectedOptionText;
			}
		}
	}

	/**
	 * shows the pop up window
	 * @param suggestions
	 */
	private void showWindow(VerticalLayout suggestions) {
		if (!popupExtension.isOpen()) {
			popupExtension.setContent(suggestions);
			popupExtension.open();
		}
	}

	/**
	 * hides the pop window
	 */
	private void hideWindow() {
		if (popupExtension.isOpen()) {
			popupExtension.close();
		}
	}	


    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.ui.Button.ClickListener#buttonClick(com.vaadin.ui.Button.
     * ClickEvent)
     */
  
	@Override
    public void buttonClick(final ClickEvent event) {
        if (SPUIComponetIdProvider.CUSTOM_FILTER_SAVE_ICON.equals(event.getComponent().getId())
                && manadatoryFieldsPresent()) {
            if (filterManagementUIState.isCreateFilterViewDisplayed()) {
                if (!doesAlreadyExists()) {
                    createTargetFilterQuery();
                }
            } else {
                if (!nameTextField.getValue().equals(oldFilterName)) {
                    if (!doesAlreadyExists()) {
                        updateCustomFilter();
                    }
                } else {
                    updateCustomFilter();
                }
            }
        }
    }

    private void createTargetFilterQuery() {
        final TargetFilterQuery targetFilterQuery = new TargetFilterQuery();
        targetFilterQuery.setName(nameTextField.getValue());
        targetFilterQuery.setQuery(queryTextField.getValue());
        targetFilterQueryManagement.createTargetFilterQuery(targetFilterQuery);
        notification.displaySuccess(
                i18n.get("message.create.filter.success", new Object[] { targetFilterQuery.getName() }));
        eventBus.publish(this, CustomFilterUIEvent.CREATE_TARGET_FILTER_QUERY);
    }

    private void updateCustomFilter() {
        final TargetFilterQuery targetFilterQuery = filterManagementUIState.getTfQuery().get();
        targetFilterQuery.setName(nameTextField.getValue());
        targetFilterQuery.setQuery(queryTextField.getValue());
        final TargetFilterQuery updatedTargetFilter = targetFilterQueryManagement
                .updateTargetFilterQuery(targetFilterQuery);
        filterManagementUIState.setTfQuery(updatedTargetFilter);
        oldFilterName = nameTextField.getValue();
        oldFilterQuery = queryTextField.getValue();
        notification.displaySuccess(i18n.get("message.update.filter.success"));
        eventBus.publish(this, CustomFilterUIEvent.UPDATED_TARGET_FILTER_QUERY);
    }

    private boolean hasSavePermission() {
        if (filterManagementUIState.isCreateFilterViewDisplayed()) {
            return permissionChecker.hasCreateTargetPermission();
        } else {
            return permissionChecker.hasUpdateTargetPermission();
        }
    }

    private boolean doesAlreadyExists() {
        if (targetFilterQueryManagement.findTargetFilterQueryByName(nameTextField.getValue()) != null) {
            notification.displayValidationError(i18n.get("message.target.filter.duplicate", nameTextField.getValue()));
            return true;
        }
        return false;
    }

    private boolean manadatoryFieldsPresent() {
        if (Strings.isNullOrEmpty(nameTextField.getValue())
                || Strings.isNullOrEmpty(filterManagementUIState.getFilterQueryValue())) {
            notification.displayValidationError(i18n.get("message.target.filter.validation"));
            return false;
        }
        return true;
    }

    private void updateStatusIconAfterTablePopulated() {
        queryTextField.focus();
        if (!validationFailed && !Strings.isNullOrEmpty(queryTextField.getValue())) {
            showValidationSuccesIcon();
        }
    }

    private void showCustomFiltersView() {
        eventBus.publish(this, CustomFilterUIEvent.SHOW_FILTER_MANAGEMENT);
    }

}
