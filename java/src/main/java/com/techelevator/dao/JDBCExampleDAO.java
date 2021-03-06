package com.techelevator.dao;

import com.techelevator.model.Example;


import javax.sql.DataSource;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import com.techelevator.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;


@Component
public class JDBCExampleDAO implements ExampleDAO{

    private JdbcTemplate jdbcTemplate;

    public JDBCExampleDAO(DataSource dataSource) {

        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }


    @Override
    public List<Example> retrieveAllExamples(int userId) {

        List<Example> examples = new ArrayList();

        if (userId > 0) {

            String sql = "SELECT * FROM examples " +
                    "JOIN languages ON languages.language_id = examples.language_id " +
                    "WHERE is_default = true OR is_private = false OR user_id = ?";

            SqlRowSet results = jdbcTemplate.queryForRowSet(sql, userId);

            while (results.next()) {
                examples.add(mapRowToExample(results));
            }
        }
        else {
            String sql = "SELECT * FROM examples " +
                    "JOIN languages ON languages.language_id = examples.language_id " +
                    "WHERE is_default = true";

            SqlRowSet results = jdbcTemplate.queryForRowSet(sql);

            while (results.next()) {
                examples.add(mapRowToExample(results));
            }

        }

        return examples;
    }

    @Override
    public void addExample(Example example) {

        int exampleId = getNextExampleId();


        //setting the language_id

        String sql = "SELECT language_id FROM languages WHERE language_name = ?";

        SqlRowSet results = jdbcTemplate.queryForRowSet(sql, example.getLanguageName());

        if (results.next()) {
            example.setLanguageId(results.getLong("language_id"));
        }

        // inserting example into examples table
        String exampleSql = "INSERT INTO examples(example_id, title, description, language_id, code_example, is_private, attribution, is_default, user_id, is_favorite) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(exampleSql, exampleId, example.getTitle(), example.getDescription(), example.getLanguageId(), example.getCodeExample(), example.isPrivateExample(), example.getAttribution(), example.isDefaultExample(), example.getUserId(), example.isFavoriteExample());

        // get list of tags from JSON, loop through, unpack them

        List<String> tagList = example.getTags();

        for (String tag : tagList) {

            String sqlTag = "SELECT tag_id FROM tags WHERE tag_name = ?";
            SqlRowSet tagResults = jdbcTemplate.queryForRowSet(sqlTag, tag);

            if (tagResults.next()) {
                int tagId = tagResults.getInt("tag_id");

        // inserting into examples_tags table

                String tagSql = "INSERT INTO examples_tags(example_id, tag_id) VALUES(?, ?)";
                jdbcTemplate.update(tagSql, exampleId, tagId);
            }
        }
    }

    @Override
    public void deleteExample(int exampleId){

        String sqlTags = "DELETE FROM examples_tags WHERE example_id = ?; " +
                "DELETE FROM examples WHERE example_id = ?";

        jdbcTemplate.update(sqlTags, exampleId, exampleId);

    }

    public void toggleFavorite(int exampleId){
        String sql = "UPDATE EXAMPLES SET is_favorite = NOT is_favorite WHERE example_id = ?";

        jdbcTemplate.update(sql, exampleId);
    }

    @Override
    public void editExample(Example example) {


        String sqlLanguageName = "SELECT language_id FROM languages WHERE language_name = ?";

        SqlRowSet results = jdbcTemplate.queryForRowSet(sqlLanguageName, example.getLanguageName());

        if (results.next()) {
            example.setLanguageId(results.getLong("language_id"));
        }

        String sql = "UPDATE examples " +
                "SET title = ?, " +
                "description = ?, " +
                "language_id = ?, " +
                "code_example = ?, " +
                "attribution = ?, " +
                "is_private = ?, " +
                "is_default = ? " +
                "WHERE example_id = ?";



        jdbcTemplate.update(sql, example.getTitle(), example.getDescription(), example.getLanguageId(), example.getCodeExample(), example.getAttribution(),
                example.isPrivateExample(), example.isDefaultExample(), example.getExampleId());


        String deleteTags = "DELETE FROM examples_tags WHERE example_id = ?";

        jdbcTemplate.update(deleteTags, example.getExampleId());

        List<String> tagList = example.getTags();

        for (String tag : tagList) {

            String sqlTag = "SELECT tag_id FROM tags WHERE tag_name = ?";
            SqlRowSet tagResults = jdbcTemplate.queryForRowSet(sqlTag, tag);

            if (tagResults.next()) {
                int tagId = tagResults.getInt("tag_id");

                // inserting into examples_tags table


                String tagSql = "INSERT INTO examples_tags(example_id, tag_id) VALUES(?, ?)";
                jdbcTemplate.update(tagSql, example.getExampleId(), tagId);
            }
        }
    }

    private Example mapRowToExample(SqlRowSet results) {

        Example example = new Example();

        example.setTitle(results.getString("title"));
        example.setDescription(results.getString("description"));
        example.setTags(retrieveTags(results.getLong("example_id")));
        example.setExampleId(results.getLong("example_id"));
        example.setLanguageName(results.getString("language_name"));
        example.setCodeExample(results.getString("code_example"));
        example.setPrivateExample(results.getBoolean("is_private"));
        example.setAttribution(results.getString("attribution"));
        example.setUserId(results.getLong("user_id"));
        example.setDefaultExample(results.getBoolean("is_default"));
        example.setFavoriteExample(results.getBoolean("is_favorite"));

        return example;

    }

    private List<String> retrieveTags(long exampleId) {
        List<String> allTags = new ArrayList<>();

        String sql = "SELECT * FROM examples " +
                "JOIN examples_tags ON examples_tags.example_id = examples.example_id " +
                "JOIN tags ON tags.tag_id = examples_tags.tag_id " +
                "WHERE examples.example_id = ?";

        SqlRowSet results = jdbcTemplate.queryForRowSet(sql, exampleId);

        while (results.next()) {
            allTags.add(results.getString("tag_name"));
        }



        return allTags;

    }

    private int getNextExampleId() {
        SqlRowSet nextId = jdbcTemplate.queryForRowSet("SELECT nextval('seq_example_id')");
        if(nextId.next()) {
            return nextId.getInt(1);
        } else {
            throw new RuntimeException("Something went wrong getting an id for the new example");
        }
    }

}
