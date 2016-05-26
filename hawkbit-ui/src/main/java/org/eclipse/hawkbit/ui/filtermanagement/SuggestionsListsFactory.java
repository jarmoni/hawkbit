package org.eclipse.hawkbit.ui.filtermanagement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.hawkbit.repository.TargetFields;

public class SuggestionsListsFactory {

	private static List<SuggestionDto> TOKENS = null;
	private static List<SuggestionDto> RELATIONAL_OPERATORS = null;
	private static List<SuggestionDto> Values = null;
	private static List<SuggestionDto> LOGICAL_OPERATORS = null;

	static{
		if(TOKENS == null){
			TOKENS = new ArrayList<SuggestionDto>();
			createTokens();
		}
		if(RELATIONAL_OPERATORS == null){
			RELATIONAL_OPERATORS = new ArrayList<SuggestionDto>();
			createRelationalOpeartors();
		}

		createValues();

		if(LOGICAL_OPERATORS == null){
			LOGICAL_OPERATORS = new ArrayList<SuggestionDto>();
			createLogicalOperators();
		}
	}

	public static List<SuggestionDto> getTOKENS() {
		return TOKENS;
	}




	public static List<SuggestionDto> getRELATIONAL_OPERATORS() {
		return RELATIONAL_OPERATORS;
	}




	public static List<SuggestionDto> getValues() {
		return Values = new ArrayList<>();
	}


	public static List<SuggestionDto> getLOGICAL_OPERATORS() {
		return LOGICAL_OPERATORS;
	}
	
	public static List<SuggestionDto> getSuggestionListFromType(String type){
		if(type != null){
			switch (type) {
			case "TOKENS":
				return getTOKENS();
			case "RELATIONAL_OPERATORS":
				return getRELATIONAL_OPERATORS();
			case "VALUES":
				return getValues();
			case "LOGICAL_OPERATORS":
				return getLOGICAL_OPERATORS();
			default:
				return null;
			}
		}
		return new ArrayList<>();
		
	}


	private static void createTokens() {
		int i =1;
		for(String targetFields : Stream.of(TargetFields.values()).map(TargetFields::name).collect(Collectors.toList())){
			TOKENS.add(new SuggestionDto(i, targetFields));
			i++;
		}

	}

	private static void createRelationalOpeartors() {
		RELATIONAL_OPERATORS.add(new SuggestionDto(1, "=="));
		RELATIONAL_OPERATORS.add(new SuggestionDto(1, "!="));
		RELATIONAL_OPERATORS.add(new SuggestionDto(1, "IN"));
	}


	private static void createValues() {
		// TODO Auto-generated method stub

	}

	private static void createLogicalOperators() {
		LOGICAL_OPERATORS.add(new SuggestionDto(1, "AND"));
		LOGICAL_OPERATORS.add(new SuggestionDto(2, "OR"));
	}


}
