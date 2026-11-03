package com.kickstats.service;

import com.kickstats.dto.StadiumForm;
import com.kickstats.entity.Stadium;
import com.kickstats.exception.DuplicateNameException;
import com.kickstats.exception.NotFoundException;
import com.kickstats.repository.StadiumRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StadiumService {

    private final StadiumRepository stadiumRepository;

    public List<Stadium> findAll() {
        return stadiumRepository.findAll(Sort.by("name"));
    }

    public StadiumForm getForm(Integer stadiumId) {
        Stadium stadium = getStadium(stadiumId);
        StadiumForm form = new StadiumForm();
        form.setName(stadium.getName());
        form.setCity(stadium.getCity());
        form.setCapacity(stadium.getCapacity());
        return form;
    }

    @Transactional
    public void create(StadiumForm form) {
        String name = form.getName().strip();
        if (stadiumRepository.existsByName(name)) {
            throw new DuplicateNameException("이미 등록된 경기장 이름입니다.");
        }
        Stadium stadium = new Stadium();
        apply(stadium, name, form);
        stadiumRepository.save(stadium);
    }

    @Transactional
    public void update(Integer stadiumId, StadiumForm form) {
        Stadium stadium = getStadium(stadiumId);
        String name = form.getName().strip();
        if (stadiumRepository.existsByNameAndStadiumIdNot(name, stadiumId)) {
            throw new DuplicateNameException("이미 등록된 경기장 이름입니다.");
        }
        apply(stadium, name, form);
    }

    // DB의 ON DELETE SET NULL로 팀과 경기의 경기장 정보만 비워진다.
    @Transactional
    public void delete(Integer stadiumId) {
        stadiumRepository.delete(getStadium(stadiumId));
    }

    private Stadium getStadium(Integer stadiumId) {
        return stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new NotFoundException("경기장을 찾을 수 없습니다: " + stadiumId));
    }

    private void apply(Stadium stadium, String name, StadiumForm form) {
        stadium.setName(name);
        stadium.setCity(blankToNull(form.getCity()));
        stadium.setCapacity(form.getCapacity());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
